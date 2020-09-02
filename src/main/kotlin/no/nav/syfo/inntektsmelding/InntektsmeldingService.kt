package no.nav.syfo.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
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
    fun start() {
        log.info("InntektsmeldingService started")
        while (applicationState.ready) {
            val consumerRecords = inntektsmeldingConsumer.poll()
            consumerRecords.forEach {
                val inntektsmelding: Inntektsmelding = objectMapper.readValue(it.value())
                database.lagreInntektsmelding(inntektsmelding)
                log.info("Lagrer ${inntektsmelding.inntektsmeldingId} i databasen")
            }
        }
    }
}
