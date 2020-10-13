package no.nav.syfo.inntektsmelding

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.finnInntektsmelding
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.domene.tilEnkelInntektsmelding
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log
import no.nav.syfo.metrikk.MOTTATT_INNTEKTSMELDING
import org.postgresql.util.PSQLException
import java.lang.Exception

@KtorExperimentalAPI
class InntektsmeldingService(
    private val database: DatabaseInterface,
    private val applicationState: ApplicationState,
    private val inntektsmeldingConsumer: InntektsmeldingConsumer,
    private val delayStart: Long = 10_000L
) {
    suspend fun start() {
        while (applicationState.alive) {
            try {
                run()
            } catch (ex: Exception) {
                log.error("Feil ved konsumering fra kafka, restarter om $delayStart ms", ex)
                inntektsmeldingConsumer.unsubscribe()
            }
            delay(delayStart)
        }
    }

    private fun run() {
        log.info("Starter InntektsmeldingService")
        inntektsmeldingConsumer.subscribe()

        while (applicationState.ready) {
            val cr = inntektsmeldingConsumer.poll()
            cr.forEach {
                val inntektsmelding: Inntektsmelding = it.value()

                lagreInntektsmelding(inntektsmelding)

                MOTTATT_INNTEKTSMELDING.inc()
            }
            if (!cr.isEmpty) {
                inntektsmeldingConsumer.commitSync()
            }
        }
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
