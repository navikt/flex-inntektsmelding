package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.Environment
import no.nav.syfo.api.registerInntektsmeldingApi
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import no.nav.syfo.inntektsmelding.InntektsmeldingService
import no.nav.syfo.log
import java.util.UUID

@KtorExperimentalAPI
fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
    jwkProvider: JwkProvider,
    issuer: String,
    loginserviceClientId: String,
    inntektsmeldingService: InntektsmeldingService
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        settOppApplication(
            loginserviceClientId = loginserviceClientId,
            jwkProvider = jwkProvider,
            issuer = issuer,
            applicationState = applicationState,
            inntektsmeldingService = inntektsmeldingService
        )
    }

@KtorExperimentalAPI
fun Application.settOppApplication(loginserviceClientId: String, jwkProvider: JwkProvider, issuer: String, applicationState: ApplicationState, inntektsmeldingService: InntektsmeldingService) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        }
    }
    setupAuth(
        loginserviceClientId = loginserviceClientId,
        jwkProvider = jwkProvider,
        issuer = issuer
    )
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            log.error("Caught exception ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
        }
    }

    routing {
        registerNaisApi(applicationState)
        authenticate("jwt") {
            registerInntektsmeldingApi(inntektsmeldingService)
        }
    }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}
