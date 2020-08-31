package no.nav.syfo.inntektsmelding

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.kafka.InntektsmeldingConsumer
import no.nav.syfo.log

class InntektsmeldingService(
    private val database: DatabaseInterface,
    private val applicationState: ApplicationState,
    private val inntektsmeldingConsumer: InntektsmeldingConsumer
) {
    fun start() {
        log.info("InntektsmeldingService started")

        while(applicationState.ready) {
            val consumerRecords = inntektsmeldingConsumer.poll()
            consumerRecords?.forEach {
                log.info("Inntektsmelding record", it)
            }
        }
    }
}
