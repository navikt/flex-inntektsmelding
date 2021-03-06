package no.nav.syfo.kafka

import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class InntektsmeldingConsumer(
    private val kafkaInntektsmeldingConsumer: KafkaConsumer<String, Inntektsmelding>,
    private val topics: List<String>
) {
    fun subscribe() {
        kafkaInntektsmeldingConsumer.subscribe(topics)
    }

    fun unsubscribe() {
        kafkaInntektsmeldingConsumer.unsubscribe()
    }

    fun poll(): ConsumerRecords<String, Inntektsmelding> {
        return kafkaInntektsmeldingConsumer.poll(Duration.ofMillis(1000))
    }

    fun commitSync() {
        kafkaInntektsmeldingConsumer.commitSync()
    }
}
