package no.nav.syfo.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log
import no.nav.syfo.objectMapper

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
                // TODO: Siden mye lages som json, trenger man kanskje ikke å mappe her...
                val inntektsmelding: Inntektsmelding = objectMapper.readValue(it.value())
                database.lagreInntektsmelding(inntektsmelding)
                log.info("Lagrer ${inntektsmelding.inntektsmeldingId} i databasen")
            }
        }
        delay(1)
    }
}
