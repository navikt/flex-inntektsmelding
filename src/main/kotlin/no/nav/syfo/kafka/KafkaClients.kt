package no.nav.syfo.kafka

import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.Environment
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaClients(env: Environment) {
    val kafkaInntektsmeldingConsumer = getInntektsmeldingConsumer(env)

    private fun getInntektsmeldingConsumer(env: Environment): KafkaConsumer<String, Inntektsmelding> {
        val config = loadBaseConfig(env, env.hentKafkaCredentials()).envOverrides()
        config["auto.offset.reset"] = "latest" // TODO: Sett denne til earliest

        val properties = config.toConsumerConfig(
            groupId = "${env.applicationName}-consumer", // TODO: Endre denne
            keyDeserializer = StringDeserializer::class,
            valueDeserializer = JacksonKafkaDeserializer::class
        )
        properties.let { it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1" }

        val consumer = KafkaConsumer<String, Inntektsmelding>(properties)
        consumer.subscribe(listOf(env.inntektsmeldingTopics))

        return consumer
    }
}
