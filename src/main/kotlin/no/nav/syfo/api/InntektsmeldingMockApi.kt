package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.Environment
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.db.slettInntektsmelding

@KtorExperimentalAPI
fun Route.registerInntektsmeldingMockApi(database: DatabaseInterface, env: Environment) {
    route("/api/v1/mock") {
        post("/inntektsmeldinger/{fnr}") {
            if (env.isProd()) {
                throw IllegalStateException("Dette apiet er ikke på i produksjon")
            }

            val inntektsmelding = call.receive<Inntektsmelding>()

            database.lagreInntektsmelding(inntektsmelding)

            call.respond(Melding("Inntektsmelding med id ${inntektsmelding.inntektsmeldingId} opprettet").tilRespons(HttpStatusCode.Created))
        }

        delete("/inntektsmeldinger/{fnr}") {
            if (env.isProd()) {
                throw IllegalStateException("Dette apiet er ikke på i produksjon")
            }
            val fnr = call.parameters["fnr"]!!
            val inntektsmeldinger = database.finnInntektsmeldinger(fnr)
            inntektsmeldinger.forEach {
                database.slettInntektsmelding(it.inntektsmeldingId)
            }

            call.respond(Melding("Slettet ${inntektsmeldinger.size} inntektsmeldinger").tilRespons(HttpStatusCode.OK))
        }
    }
}
