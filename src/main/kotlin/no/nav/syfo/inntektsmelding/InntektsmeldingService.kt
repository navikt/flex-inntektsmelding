package no.nav.syfo.inntektsmelding

import io.ktor.util.KtorExperimentalAPI
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.finnInntektsmelding
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.domene.tilEnkelInntektsmelding
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log
import no.nav.syfo.metrikk.MOTTATT_INNTEKTSMELDING
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.postgresql.util.PSQLException

@KtorExperimentalAPI
class InntektsmeldingService(
    private val env: Environment,
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

                lagreInntektsmelding(inntektsmelding)

                MOTTATT_INNTEKTSMELDING.inc()
            }
            consumerRecords = inntektsmeldingConsumer.poll()
        } while (applicationState.ready)
    }

    private fun lagreInntektsmelding(inntektsmelding: Inntektsmelding) {
        try {
            database.lagreInntektsmelding(inntektsmelding.tilEnkelInntektsmelding())
            log.info("Lagrer ${inntektsmelding.inntektsmeldingId} i databasen")
        } catch (e: PSQLException) {
            if (e.message?.contains("ERROR: duplicate key value violates unique constraint") == true) {
                log.warn("Inntektsmelding ${inntektsmelding.inntektsmeldingId} ligger allerede i databasen, lagrer ikke")
            } else {
                throw e
            }
        }
    }
    fun finnInntektsmelding(id: String, fnr: String) = database.finnInntektsmelding(id, fnr)
    fun finnInntektsmeldinger(fnr: String) = database.finnInntektsmeldinger(fnr)
}
