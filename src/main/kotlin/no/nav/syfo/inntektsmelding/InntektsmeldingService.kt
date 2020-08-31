package no.nav.syfo.inntektsmelding

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log

@KtorExperimentalAPI
class InntektsmeldingService(
    private val database: DatabaseInterface,
    private val applicationState: ApplicationState,
    private val inntektsmeldingConsumer: InntektsmeldingConsumer
) {
    suspend fun start() {
        log.info("InntektsmeldingService started")

        while (applicationState.ready) {
            val consumerRecords = inntektsmeldingConsumer.poll()
            consumerRecords.forEach {
                log.info("Inntektsmelding record", it)
            }
        }
        delay(1)
    }
}
