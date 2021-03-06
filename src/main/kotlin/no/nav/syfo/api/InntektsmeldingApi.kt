package no.nav.syfo.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.domene.EnkelInntektsmelding
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import java.math.BigDecimal

@KtorExperimentalAPI
fun Route.registerInntektsmeldingApi(inntektsmeldingService: InntektsmeldingService) {
    route("/api/v1") {
        get("inntektsmeldinger") {
            val fnr = call.fnr()
            val inntektsmeldinger = inntektsmeldingService.finnInntektsmeldinger(fnr)
            call.respond(inntektsmeldinger.map { it.tilRSInntektsmelding() })
        }

        get("inntektsmeldinger/{id}") {
            val id = call.parameters["id"]!!
            val fnr = call.fnr()
            val inntektsmelding = inntektsmeldingService.finnInntektsmelding(id, fnr)
            if (inntektsmelding == null) {
                call.respond(Melding("Finner ikke inntektsmelding $id").tilRespons(HttpStatusCode.NotFound))
            } else {
                call.respond(inntektsmelding.tilRSInntektsmelding())
            }
        }
    }
}

fun ApplicationCall.fnr(): String {
    val principal: JWTPrincipal = this.authentication.principal()!!
    return principal.payload.subject
}

data class Melding(
    val melding: String
)

private fun Melding.toJson() = ObjectMapper().writeValueAsString(this)

fun Melding.tilRespons(httpStatusCode: HttpStatusCode = HttpStatusCode.InternalServerError) =
    io.ktor.http.content.TextContent(
        this.toJson(),
        io.ktor.http.ContentType.Application.Json.withCharset(Charsets.UTF_8),
        httpStatusCode
    )

data class RSInntektsmelding(
    val id: String,
    @JsonIgnore
    val beregnetInntekt: BigDecimal?

) {
    @JsonSerialize
    fun månedsinntekt(): Int? {
        return beregnetInntekt?.toInt()
    }

    @JsonSerialize
    fun årsinntekt(): Int? {
        beregnetInntekt?.let {
            return it.toInt() * 12
        }
        return null
    }
}

fun EnkelInntektsmelding.tilRSInntektsmelding() =
    RSInntektsmelding(
        id = inntektsmeldingId,
        beregnetInntekt = beregnetInntekt
    )
