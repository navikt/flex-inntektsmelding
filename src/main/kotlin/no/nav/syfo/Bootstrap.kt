package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.db.Database
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.kafka.KafkaClients
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.spinnsyn-backend")

@KtorExperimentalAPI
fun main() {
    log.info("Starter flex-inntektsmelding")
    val env = Environment()
    val applicationState = ApplicationState()

    val database = Database(env)
    val kafkaClients = KafkaClients(env)
    DefaultExports.initialize()


    val inntektsmeldingConsumer = InntektsmeldingConsumer(kafkaClients.kafkaInntektsmeldingConsumer)
    val inntekstmeldingService = InntektsmeldingService(
        database = database,
        applicationState = applicationState,
        inntektsmeldingConsumer = inntektsmeldingConsumer
    )

    val applicationEngine = createApplicationEngine(
        env = env,
        applicationState = applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
    log.info("Application server stated")
    createListener(applicationState) {
        inntekstmeldingService.start()
    }
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
