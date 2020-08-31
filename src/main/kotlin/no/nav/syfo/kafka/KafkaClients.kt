package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.Environment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import kotlin.reflect.KClass

class KafkaClients(env: Environment) {
    val kafkaInntektsmeldingConsumer = getInntektsmeldingConsumer(env)

    private fun getInntektsmeldingConsumer(env: Environment): KafkaConsumer<String, Inntektsmelding> {
        val config = loadBaseConfig(env, env.hentKafkaCredentials()).envOverrides()
        config["auto.offset.reset"] = "latest"

        val properties = config.toConsumerConfig("${env.applicationName}-consumer", KafkaValueDeserializer::class)
        properties.let { it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1" }

        val consumer = KafkaConsumer<String, Inntektsmelding>(properties, StringDeserializer(), KafkaValueDeserializer(Inntektsmelding::class))
        consumer.subscribe(listOf(env.inntektsmeldingTopics))

        return consumer
    }

    private class KafkaValueDeserializer<T : Any> (private val type: KClass<T>) : Deserializer<T> {
        private val objectMapper: ObjectMapper = JacksonJsonConfig.opprettObjectMapper()

        override fun configure(configs: MutableMap<String, *>, isKey: Boolean) { }

        override fun deserialize(topic: String?, data: ByteArray): T {
            return objectMapper.readValue(data, type.java)
        }

        override fun close() { }
    }
}
