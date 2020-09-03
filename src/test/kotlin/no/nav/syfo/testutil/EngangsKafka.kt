package no.nav.syfo.testutil

import io.mockk.every
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.application.ApplicationState
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun stopApplicationNårKafkaTopicErLest(
    kafkaConsumer: KafkaConsumer<String, Inntektsmelding>,
    applicationState: ApplicationState
) {
    every { kafkaConsumer.poll(any<Duration>()) } answers {
        val cr = callOriginal()
        if (!cr.isEmpty) {
            applicationState.ready = false
            applicationState.alive = false
        }
        cr
    }
}
