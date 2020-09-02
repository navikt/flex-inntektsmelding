package no.nav.syfo

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.server.testing.TestApplicationEngine
import io.mockk.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.runBlocking
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.testutil.TestDB
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import java.math.BigDecimal
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.* // ktlint-disable no-wildcard-imports

object InntektsmeldingSpek : Spek({
    val env = mockk<Environment>()
    val inntektsmelding = Inntektsmelding(
        inntektsmeldingId = "1",
        status = Status.GYLDIG,
        arbeidstakerFnr = "12345678901",
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
        clearAllMocks()
        every { env.serviceuserUsername } returns "srvflexim"
    }

    // TODO: Finn ut hvorfor denne feiler, trolig noe med EmbeddedPostgres
    xdescribe("Tester konsumering av inntektsmeldinger") {
        with(TestApplicationEngine()) {
            val database = TestDB()
            val kafka = KafkaContainer().withNetwork(Network.newNetwork())
            kafka.start()
            val kafkaConfig = Properties()
            kafkaConfig.let {
                it["bootstrap.servers"] = kafka.bootstrapServers
                it[ConsumerConfig.GROUP_ID_CONFIG] = "groupId"
                it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }

            val producerProperties = kafkaConfig.toProducerConfig(groupId = "producer", valueSerializer = StringSerializer::class)
            val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
            val inntektsmeldingKafkaProducer = KafkaProducer<String, String>(producerProperties)

            val inntektsmeldingProperties = kafkaConfig.toConsumerConfig(groupId = "consumer", valueDeserializer = StringDeserializer::class)
            val inntektsmeldingKafkaConsumer = spyk(KafkaConsumer<String, String>(inntektsmeldingProperties))
                .apply { subscribe(listOf("privat-sykepenger-inntektsmelding")) }
            val inntektsmeldingConsumer = InntektsmeldingConsumer(inntektsmeldingKafkaConsumer)

            val applicationState = ApplicationState().apply {
                ready = true
                alive = true
            }
            val inntektsmeldingService = InntektsmeldingService(
                database = database,
                applicationState = applicationState,
                inntektsmeldingConsumer = inntektsmeldingConsumer
            )

            val fnr = "12345678901"
            start()
            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }

            it("Inntektsmelding mottas fra kafka topic og lagres i db") {
                val ingenInntektsmelding = database.finnInntektsmeldinger(fnr)
                ingenInntektsmelding.size `should be equal to` 0

                runBlocking {
                    inntektsmeldingService.start()
                }

                inntektsmeldingKafkaProducer.send(
                    ProducerRecord(
                        "privat-sykepenger-inntektsmelding",
                        null,
                        fnr,
                        objectMapper.writeValueAsString(inntektsmelding),
                        emptyList()
                    )
                )

                val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
                inntektsmeldinger.size `should be equal to` 1
            }
        }
    }
})
