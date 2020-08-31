package no.nav.syfo.kafka

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class InntektsmeldingConsumer(
    private val kafkaInntektsmeldingConsumer: KafkaConsumer<String, String>
) {
    fun poll(): ConsumerRecords<String, String> {
        return kafkaInntektsmeldingConsumer.poll(Duration.ofMillis(0))
    }
}
