package no.nav.syfo.inntektsmelding

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log

@KtorExperimentalAPI
class InntektsmeldingService(
    private val database: DatabaseInterface,
    private val applicationState: ApplicationState,
    private val inntektsmeldingConsumer: InntektsmeldingConsumer
) {
    suspend fun start() {
        log.info("Pre poll")
        inntektsmeldingConsumer.poll()
        log.info("Venter på broker")
        delay(30000)
        log.info("Seek 1 tilbake")
        inntektsmeldingConsumer.startFraForrige()
        while (applicationState.ready) {
            log.info("App skal være på bena?")
            val consumerRecords = inntektsmeldingConsumer.poll()
            consumerRecords.forEach {
                val inntektsmelding: Inntektsmelding = it.value()
                database.lagreInntektsmelding(inntektsmelding)
                log.info("Lagrer ${inntektsmelding.inntektsmeldingId} i databasen")
            }
        }
    }
}
