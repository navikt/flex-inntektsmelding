package no.nav.syfo.kafka.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.common.serialization.Deserializer

class JacksonKafkaDeserializer : Deserializer<Inntektsmelding> {
    private val objectMapper: ObjectMapper = JacksonJsonConfig.opprettObjectMapper()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) { }

    override fun deserialize(topic: String?, data: ByteArray): Inntektsmelding {
        return objectMapper.readValue(data)
    }

    // nothing to close
    override fun close() { }
}
