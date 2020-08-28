package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.spinnsyn-backend")

@KtorExperimentalAPI
fun main() {
    val env = Environment()

    val applicationState = ApplicationState()

    DefaultExports.initialize()

    val applicationEngine = createApplicationEngine(
        env = env,
        applicationState = applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
    log.info("Application server stated")
}
