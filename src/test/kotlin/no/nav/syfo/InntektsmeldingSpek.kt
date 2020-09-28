package no.nav.syfo

import io.ktor.util.* // ktlint-disable no-wildcard-imports
import io.mockk.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.runBlocking
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.testutil.TestDB
import no.nav.syfo.testutil.stopApplicationNårKafkaTopicErLest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
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
import java.math.BigDecimal
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.* // ktlint-disable no-wildcard-imports

@KtorExperimentalAPI
object InntektsmeldingSpek : Spek({
    val env = mockkClass(Environment::class)
    val database = TestDB()

    every { env.applicationName } returns "application"
    every { env.inntektsmeldingTopics } returns "topic"

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
    kafkaConsumer.subscribe(listOf(env.inntektsmeldingTopics))
    val inntektsmeldingConsumer = InntektsmeldingConsumer(kafkaConsumer)
    val applicationState = ApplicationState(alive = true, ready = true)

    val inntektsmeldingService = InntektsmeldingService(
        env = env,
        database = database,
        applicationState = applicationState,
        inntektsmeldingConsumer = inntektsmeldingConsumer
    )
    val fnr = "12345678901"
    val inntektsmelding = Inntektsmelding(
        inntektsmeldingId = "1",
        status = Status.GYLDIG,
        arbeidstakerFnr = fnr,
        arbeidstakerAktorId = "aktorID",
        mottattDato = LocalDateTime.now(),
        arbeidsgivertype = Arbeidsgivertype.PRIVAT,
        foersteFravaersdag = now().minusDays(10),
        arbeidsgiverperioder = listOf(
            Periode(
                fom = now().minusDays(10),
                tom = now()
            )
        ),
        arkivreferanse = "999",
        refusjon = Refusjon(
            beloepPrMnd = BigDecimal(1000),
            opphoersdato = now()
        ),
        endringIRefusjoner = emptyList(),
        ferieperioder = emptyList(),
        gjenopptakelseNaturalytelser = emptyList(),
        opphoerAvNaturalytelser = emptyList()
    )

    beforeEachTest {
        applicationState.ready = true
        applicationState.alive = true
    }

    describe("Tester konsumering av inntektsmeldinger") {
        it("Inntektsmelding mottas fra kafka topic og lagres i db") {
            val ingenInntektsmelding = database.finnInntektsmeldinger(fnr)
            ingenInntektsmelding.size `should be equal to` 0

            every { env.cluster } returns "dev-gcp"
            kafkaProducer.send(
                ProducerRecord(
                    env.inntektsmeldingTopics,
                    objectMapper.writeValueAsString(inntektsmelding)
                )
            )

            stopApplicationNårKafkaTopicErLest(kafkaConsumer, applicationState)
            runBlocking {
                inntektsmeldingService.start()
            }

            val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
            inntektsmeldinger.size `should be equal to` 1
            inntektsmeldinger[0].arkivreferanse `should be equal to` "999"
        }

        it("Lagrer ikke inntektsmelding i prod-gcp") {
            every { env.cluster } returns "prod-gcp"
            kafkaProducer.send(
                ProducerRecord(
                    env.inntektsmeldingTopics,
                    objectMapper.writeValueAsString(inntektsmelding.copy(inntektsmeldingId = "ny"))
                )
            )

            stopApplicationNårKafkaTopicErLest(kafkaConsumer, applicationState)
            runBlocking {
                inntektsmeldingService.start()
            }

            val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
            inntektsmeldinger.size `should be equal to` 1
            inntektsmeldinger[0].inntektsmeldingId `should not be equal to` "ny"
        }

        it("Håndtering av duplikate inntektsmeldinger") {
            every { env.cluster } returns "dev-gcp"
            kafkaProducer.send(
                ProducerRecord(
                    env.inntektsmeldingTopics,
                    objectMapper.writeValueAsString(inntektsmelding.copy(begrunnelseForReduksjonEllerIkkeUtbetalt = "duplikat"))
                )
            )

            stopApplicationNårKafkaTopicErLest(kafkaConsumer, applicationState)
            runBlocking {
                inntektsmeldingService.start()
            }

            val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
            inntektsmeldinger.size `should be equal to` 1
            inntektsmeldinger[0].begrunnelseForReduksjonEllerIkkeUtbetalt?.`should not be equal to`("duplikat")
        }
    }
})
