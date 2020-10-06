package no.nav.syfo

import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    override val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val inntektsmeldingTopics: String = getEnvVar("INNTEKTSMELDING_TOPICS", "privat-sykepenger-inntektsmelding"),
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),
    val flexInntektsmeldingBackendDbHost: String = getEnvVar("NAIS_DATABASE_FLEX_INNTEKTSMELDING_INNTEKTSMELDINGDB_HOST"),
    val flexInntektsmeldingBackendDbPort: String = getEnvVar("NAIS_DATABASE_FLEX_INNTEKTSMELDING_INNTEKTSMELDINGDB_PORT"),
    val flexInntektsmeldingBackendDbName: String = getEnvVar("NAIS_DATABASE_FLEX_INNTEKTSMELDING_INNTEKTSMELDINGDB_DATABASE"),
    val flexInntektsmeldingBackendDbUsername: String = getEnvVar("NAIS_DATABASE_FLEX_INNTEKTSMELDING_INNTEKTSMELDINGDB_USERNAME"),
    val flexInntektsmeldingBackendDbPassword: String = getEnvVar("NAIS_DATABASE_FLEX_INNTEKTSMELDING_INNTEKTSMELDINGDB_PASSWORD"),
    val sidecarInitialDelay: Long = getEnvVar("SIDECAR_INITIAL_DELAY", "15000").toLong(),
    val oidcWellKnownUri: String = getEnvVar("OIDC_WELLKNOWN_URI"),
    val loginserviceClientId: String = getEnvVar("LOGINSERVICE_CLIENTID"),
    val electorPath: String = getEnvVar("ELECTOR_PATH")
) : KafkaConfig {

    fun hentKafkaCredentials(): KafkaCredentials {
        return object : KafkaCredentials {
            override val kafkaPassword: String
                get() = serviceuserPassword
            override val kafkaUsername: String
                get() = serviceuserUsername
        }
    }

    fun jdbcUrl(): String {
        return "jdbc:postgresql://$flexInntektsmeldingBackendDbHost:$flexInntektsmeldingBackendDbPort/$flexInntektsmeldingBackendDbName"
    }

    fun isProd(): Boolean {
        return cluster == "prod-gcp"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
