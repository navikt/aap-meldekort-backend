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
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.komponenter.server.TOKENX
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.journalføring.motor.ArenaJournalføringJobbUtfører
import no.nav.aap.journalføring.motor.JournalføringLogInfoProvider
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class HttpServer

fun startHttpServer(
    port: Int,
    prometheus: PrometheusMeterRegistry,
    applikasjonsVersjon: String,
    tokenxConfig: TokenxConfig,
    dataSource: DataSource
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
            prometheus = prometheus,
            infoModel = InfoModel(
                title = "AAP - Meldekort",
                version = applikasjonsVersjon,
                description = """
                            For å teste API i dev, besøk
                            <a href="https://tokenx-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:aap:meldekort-backend">Token Generator</a> for å få token.
                            """.trimIndent(),
            ),
            tokenxConfig = tokenxConfig,
        )

        val motor = startMotor(dataSource, prometheus)

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
            authenticate(TOKENX) {
                apiRouting {
                    ansvarligSystemApi()
                    arenaApi(dataSource)
                    kelvinApi()
                    motorApi(dataSource)
                }
            }
            actuator(prometheus, motor)
        }
    }.start(wait = true)
}

private fun Application.startMotor(
    dataSource: DataSource,
    prometheus: PrometheusMeterRegistry
): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = 4,
        logInfoProvider = JournalføringLogInfoProvider,
        jobber = listOf(ArenaJournalføringJobbUtfører),
        prometheus = prometheus,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
    return motor
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor) {
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

        get("/ready") {
            if (motor.kjører()) {
                val status = HttpStatusCode.OK
                call.respond(status, "Oppe!")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Kjører ikke")
            }
        }
    }
}
