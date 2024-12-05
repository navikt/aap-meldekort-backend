package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.meldekort.arena.MeldekortService
import no.nav.aap.meldekort.arena.meldekortApi
import org.slf4j.LoggerFactory

class HttpServer

fun startHttpServer(
    port: Int,
    prometheus: PrometheusMeterRegistry,
    meldekortService: MeldekortService,
    applikasjonsVersjon: String,
) {
    embeddedServer(Netty, configure = {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
        connector {
            this.port = port
        }
    }) {
        commonKtorModule(
            prometheus,
            AzureConfig(),
            InfoModel(
                title = "AAP - Meldekort",
                version = applikasjonsVersjon,
                description = """
                For å teste API i dev, besøk
                <a href="https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:behandlingsflyt">Token Generator</a> for å få token.
                """.trimIndent(),
            )
        )
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                val logger = LoggerFactory.getLogger(HttpServer::class.java)
                when (cause) {
                    is ManglerTilgangException -> {
                        logger.warn("Mangler tilgang til å vise route: '{}'", call.request.local.uri, cause)
                        call.respondText(status = HttpStatusCode.Forbidden, text = "Forbidden")
                    }

                    is IkkeFunnetException -> {
                        logger.warn("Fikk 404 fra ekstern integrasjon.", cause)
                        call.respondText(status = HttpStatusCode.NotFound, text = "Ikke funnet")
                    }

                    else -> {
                        logger.warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorRespons(cause.message)
                        )
                    }
                }
            }
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }
        routing {
            authenticate(AZURE) {
                apiRouting {
                    meldekortApi(meldekortService)
                }
            }
            actuator(prometheus)
        }
    }.start(wait = true)
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }
    }
}
