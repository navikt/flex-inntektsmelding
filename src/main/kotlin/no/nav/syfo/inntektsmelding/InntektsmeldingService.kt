package no.nav.syfo.inntektsmelding

import io.ktor.util.KtorExperimentalAPI
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.finnInntektsmelding
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log
import org.apache.kafka.clients.consumer.ConsumerRecords

@KtorExperimentalAPI
class InntektsmeldingService(
    private val database: DatabaseInterface,
    private val applicationState: ApplicationState,
    private val inntektsmeldingConsumer: InntektsmeldingConsumer
) {
    fun start() {
        log.info("Starter InntektsmeldingService")

        val cr = ventTilKlar()

        log.info("Inntektsmelding consumer er klar")

        listen(cr)

        log.info("Avslutter InntektsmeldingService")
    }

    private fun ventTilKlar(): ConsumerRecords<String, Inntektsmelding> {
        var cr: ConsumerRecords<String, Inntektsmelding>
        do {
            cr = inntektsmeldingConsumer.poll()
        } while (!inntektsmeldingConsumer.erKlar() && applicationState.ready)
        return cr
    }

    private fun listen(cr: ConsumerRecords<String, Inntektsmelding>) {
        var consumerRecords = cr
        do {
            consumerRecords.forEach {
                val inntektsmelding: Inntektsmelding = it.value()
                database.lagreInntektsmelding(inntektsmelding)
                log.info("Lagrer ${inntektsmelding.inntektsmeldingId} i databasen")
            }
            consumerRecords = inntektsmeldingConsumer.poll()
        } while (applicationState.ready)
    }

    fun finnInntektsmelding(id: String, fnr: String) = database.finnInntektsmelding(id, fnr)
    fun finnInntektsmeldinger(fnr: String) = database.finnInntektsmeldinger(fnr)
}
