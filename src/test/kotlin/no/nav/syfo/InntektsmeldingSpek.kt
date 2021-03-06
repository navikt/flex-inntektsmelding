package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.settOppApplication
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.testutil.TestDB
import no.nav.syfo.testutil.generateJWT
import no.nav.syfo.testutil.settOppInntektsmelding
import no.nav.syfo.testutil.stopApplicationNårAntallKafkaMeldingerErLest
import no.nav.syfo.testutil.stopApplicationNårAntallKafkaPollErGjort
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldEqual
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.testcontainers.containers.KafkaContainer
import java.nio.file.Paths
import java.time.Duration
import java.util.Properties

@KtorExperimentalAPI
object InntektsmeldingSpek : Spek({
    val env = mockkClass(Environment::class)
    val database = TestDB()

    val issuer = "TestIssuer"
    val audience = "AUD"

    every { env.applicationName } returns "application"
    every { env.inntektsmeldingTopics } returns "topic"
    every { env.isProd() } returns true

    with(TestApplicationEngine()) {

        val kafka = KafkaContainer()
        kafka.start()

        val kafkaConfig = Properties()
        kafkaConfig.let {
            it["bootstrap.servers"] = kafka.bootstrapServers
            it[ConsumerConfig.GROUP_ID_CONFIG] = "groupId"
            it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaDeserializer::class.java
            it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }

        val kafkaProducer = KafkaProducer<String, String>(kafkaConfig)
        val kafkaConsumer = spyk(KafkaConsumer<String, Inntektsmelding>(kafkaConfig))
        val inntektsmeldingConsumer = InntektsmeldingConsumer(
            kafkaConsumer,
            listOf(env.inntektsmeldingTopics)
        )
        val applicationState = ApplicationState(alive = true, ready = true)

        val inntektsmeldingService = InntektsmeldingService(
            database = database,
            applicationState = applicationState,
            inntektsmeldingConsumer = inntektsmeldingConsumer,
            delayStart = 100L
        )
        val fnr = "12345678901"
        val inntektsmelding = settOppInntektsmelding(fnr)

        val path = "src/test/resources/jwkset.json"
        val uri = Paths.get(path).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()

        start()

        application.settOppApplication(
            loginserviceClientId = audience,
            jwkProvider = jwkProvider,
            issuer = issuer,
            applicationState = applicationState,
            inntektsmeldingService = inntektsmeldingService,
            env = env,
            database = database
        )

        fun TestApplicationRequest.medFnr(subject: String) {
            addHeader(
                HttpHeaders.Authorization,
                "Bearer ${generateJWT(audience = audience, issuer = issuer, subject = subject)}"
            )
        }

        fun setupEnvMock() {
            clearAllMocks()
            every { env.applicationName } returns "application"
            every { env.inntektsmeldingTopics } returns "topic"
            every { env.isProd() } returns true
        }

        beforeEachTest {
            setupEnvMock()
            applicationState.ready = true
            applicationState.alive = true
        }

        describe("Tester konsumering av inntektsmeldinger") {
            it("Inntektsmelding mottas fra kafka topic og lagres i db") {
                val ingenInntektsmelding = database.finnInntektsmeldinger(fnr)
                ingenInntektsmelding.size `should be equal to` 0

                val producerRecord = ProducerRecord<String, String>(
                    env.inntektsmeldingTopics,
                    objectMapper.writeValueAsString(inntektsmelding)
                )
                kafkaProducer.send(
                    producerRecord
                )

                stopApplicationNårAntallKafkaMeldingerErLest(kafkaConsumer, applicationState, 1)
                runBlocking {
                    inntektsmeldingService.start()
                }

                verify(exactly = 1) { kafkaConsumer.commitSync() }
                val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
                inntektsmeldinger.size `should be equal to` 1
                inntektsmeldinger[0].inntektsmeldingId `should be equal to` "1"
            }

            it("Inntektsmeldinger kan hentes fra APIet") {

                with(
                    handleRequest(HttpMethod.Get, "/api/v1/inntektsmeldinger") {
                        medFnr(fnr)
                    }
                ) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldEqual "[{\"id\":\"1\",\"månedsinntekt\":50000,\"årsinntekt\":600000}]"
                }
            }

            it("Inntektsmelding kan hentes fra APIet") {

                with(
                    handleRequest(HttpMethod.Get, "/api/v1/inntektsmeldinger/1") {
                        medFnr(fnr)
                    }
                ) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldEqual "{\"id\":\"1\",\"månedsinntekt\":50000,\"årsinntekt\":600000}"
                }
            }

            it("Ukjent ID gir 404") {

                with(
                    handleRequest(HttpMethod.Get, "/api/v1/inntektsmeldinger/4554") {
                        medFnr(fnr)
                    }
                ) {
                    response.status() shouldEqual HttpStatusCode.NotFound
                    response.content shouldEqual "{\"melding\":\"Finner ikke inntektsmelding 4554\"}"
                }
            }

            it("Ingen token gir 401") {
                with(
                    handleRequest(HttpMethod.Get, "/api/v1/inntektsmeldinger/4554")
                ) {
                    response.status() shouldEqual HttpStatusCode.Unauthorized
                }
            }

            it("Håndtering av duplikate inntektsmeldinger") {
                kafkaProducer.send(
                    ProducerRecord(
                        env.inntektsmeldingTopics,
                        objectMapper.writeValueAsString(inntektsmelding.copy(begrunnelseForReduksjonEllerIkkeUtbetalt = "duplikat"))
                    )
                )

                stopApplicationNårAntallKafkaMeldingerErLest(kafkaConsumer, applicationState, 1)
                runBlocking {
                    inntektsmeldingService.start()
                }

                verify(exactly = 1) { kafkaConsumer.commitSync() }
                val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
                inntektsmeldinger.size `should be equal to` 1
            }

            it("Consumer poll kan returnere tom liste") {
                stopApplicationNårAntallKafkaPollErGjort(kafkaConsumer, applicationState, antallKafkaPoll = 2)
                val co = launch {
                    inntektsmeldingService.start()
                }
                runBlocking {
                    co.join()
                    verify(exactly = 2) { kafkaConsumer.poll(any<Duration>()) }
                    verify(exactly = 0) { kafkaConsumer.commitSync() }
                }
            }
        }
    }
})
