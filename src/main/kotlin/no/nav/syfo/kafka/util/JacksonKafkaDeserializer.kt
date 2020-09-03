package no.nav.syfo.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.common.serialization.Deserializer

class JacksonKafkaDeserializer : Deserializer<Inntektsmelding> {
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) { }

    override fun deserialize(topic: String?, data: ByteArray): Inntektsmelding {
        return objectMapper.readValue(data)
    }

    // nothing to close
    override fun close() { }
}
