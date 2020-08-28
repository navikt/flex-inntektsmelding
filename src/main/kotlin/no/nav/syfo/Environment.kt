package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),
    val flexInntektsmeldingBackendDbHost: String = getEnvVar("NAIS_DATABASE_FLEXINNTEKTSMELDING_INNTEKTSMELDINGDB_HOST"),
    val flexInntektsmeldingBackendDbPort: String = getEnvVar("NAIS_DATABASE_FLEXINNTEKTSMELDING_INNTEKTSMELDINGDB_PORT"),
    val flexInntektsmeldingBackendDbName: String = getEnvVar("NAIS_DATABASE_FLEXINNTEKTSMELDING_INNTEKTSMELDINGDB_DATABASE"),
    val flexInntektsmeldingBackendDbUsername: String = getEnvVar("NAIS_DATABASE_FLEXINNTEKTSMELDING_INNTEKTSMELDINGDB_USERNAME"),
    val flexInntektsmeldingBackendDbPassword: String = getEnvVar("NAIS_DATABASE_FLEXINNTEKTSMELDING_INNTEKTSMELDINGDB_PASSWORD")
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$flexInntektsmeldingBackendDbHost:$flexInntektsmeldingBackendDbPort/$flexInntektsmeldingBackendDbName"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
