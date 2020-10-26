package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.getWellKnown
import no.nav.syfo.db.Database
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.inntektsmelding.cronjob.settOppInntektsmeldingCronjob
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.kafka.KafkaClients
import no.nav.syfo.util.PodLeaderCoordinator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.flex-inntektsmelding")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

@KtorExperimentalAPI
fun main() {
    log.info("Starter flex-inntektsmelding")
    val env = Environment()

    // Sov litt slik at sidecars er klare
    log.info("Sover i ${env.sidecarInitialDelay} ms i håp om at sidecars er klare")
    Thread.sleep(env.sidecarInitialDelay)

    val wellKnown = getWellKnown(env.loginserviceIdportenDiscoveryUrl)

    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val applicationState = ApplicationState()

    val database = Database(env)
    val kafkaClients = KafkaClients(env)
    DefaultExports.initialize()

    val inntektsmeldingConsumer = InntektsmeldingConsumer(
        kafkaClients.kafkaInntektsmeldingConsumer,
        listOf(env.inntektsmeldingTopics)
    )
    val inntekstmeldingService = InntektsmeldingService(
        database = database,
        applicationState = applicationState,
        inntektsmeldingConsumer = inntektsmeldingConsumer
    )

    val applicationEngine = createApplicationEngine(
        env = env,
        applicationState = applicationState,
        jwkProvider = jwkProvider,
        issuer = wellKnown.issuer,
        loginserviceClientId = env.loginserviceIdportenAudience,
        inntektsmeldingService = inntekstmeldingService,
        database = database
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
    log.info("Application server started")

    createListener(applicationState) {
        inntekstmeldingService.start()
    }

    val podLeaderCoordinator = PodLeaderCoordinator(env = env)
    settOppInntektsmeldingCronjob(
        podLeaderCoordinator = podLeaderCoordinator,
        database = database,
        env = env
    )
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (ex: Exception) {
            log.error("Noe gikk galt: {}", ex.message)
        } finally {
            applicationState.alive = false
            applicationState.ready = false
        }
    }
