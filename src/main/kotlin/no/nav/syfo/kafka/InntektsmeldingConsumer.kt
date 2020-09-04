package no.nav.syfo.kafka

import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.log
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
        val assignments = kafkaInntektsmeldingConsumer.assignment()
        log.info("Assignments $assignments")
        val offsets = kafkaInntektsmeldingConsumer.endOffsets(assignments)
        log.info("Offsets er $offsets")
        for (tp: TopicPartition in assignments) {
            kafkaInntektsmeldingConsumer.seek(tp, offsets[tp]?.minus(1) ?: 0)
        }
    }
}
