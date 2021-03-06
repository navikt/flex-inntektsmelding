package no.nav.syfo.testutil

import io.mockk.every
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun stopApplicationNårAntallKafkaMeldingerErLest(
    kafkaConsumer: KafkaConsumer<String, Inntektsmelding>,
    applicationState: ApplicationState,
    antallKafkaMeldinger: Int
) {
    var i = antallKafkaMeldinger
    every { kafkaConsumer.poll(any<Duration>()) } answers {
        val cr = callOriginal()
        i -= cr.count()
        if (i <= 0) {
            applicationState.ready = false
            applicationState.alive = false
        }
        cr
    }
}

fun stopApplicationNårAntallKafkaPollErGjort(
    kafkaConsumer: KafkaConsumer<String, Inntektsmelding>,
    applicationState: ApplicationState,
    antallKafkaPoll: Int
) {
    var i = antallKafkaPoll
    every { kafkaConsumer.poll(any<Duration>()) } answers {
        val cr = callOriginal()
        i -= 1
        if (i <= 0) {
            applicationState.ready = false
            applicationState.alive = false
        }
        cr
    }
}
