package no.nav.syfo.kafka

import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

class InntektsmeldingConsumer(
    private val kafkaInntektsmeldingConsumer: KafkaConsumer<String, Inntektsmelding>
) {
    fun poll(): ConsumerRecords<String, Inntektsmelding> {
        return kafkaInntektsmeldingConsumer.poll(Duration.ofMillis(1000))
    }

    fun startFraForrige() {
        poll()
        val assignments = kafkaInntektsmeldingConsumer.assignment()
        val offsets = kafkaInntektsmeldingConsumer.endOffsets(assignments)
        for (tp: TopicPartition in assignments) {
            kafkaInntektsmeldingConsumer.seek(tp, offsets[tp]?.minus(1) ?: 0)
        }
    }
}
