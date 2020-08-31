package no.nav.syfo.kafka

import no.nav.syfo.Environment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaClients(env: Environment) {
    val kafkaInntektsmeldingConsumer = getInntektsmeldingConsumer(env)

    private fun getInntektsmeldingConsumer(env: Environment): KafkaConsumer<String, String> {
        val config = loadBaseConfig(env, env.hentKafkaCredentials()).envOverrides()
        config["auto.offset.reset"] = "latest"

        val properties = config.toConsumerConfig("${env.applicationName}-consumer", StringDeserializer::class)
        properties.let { it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1" }

        val consumer = KafkaConsumer<String, String>(properties)
        consumer.subscribe(listOf(env.inntektsmeldingTopics))

        return consumer
    }
}
