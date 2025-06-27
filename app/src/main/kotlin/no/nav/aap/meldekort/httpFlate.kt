package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.route
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
import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.journalføring.JournalføringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.personBruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.TOKENX
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.MeldeperiodeFlateFactoryImpl
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.utfylling.SlettGamleUtfyllingJobbUtfører
import no.nav.aap.utfylling.UtfyllingFlateFactoryImpl
import no.nav.aap.varsel.SendVarselJobbUtfører
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class HttpServer

fun startHttpServer(
    port: Int,
    prometheus: PrometheusMeterRegistry,
    applikasjonsVersjon: String,
    tokenxConfig: TokenxConfig,
    azureConfig: AzureConfig,
    dataSource: DataSource,
    wait: Boolean = true,
    repositoryRegistry: RepositoryRegistry,
    clock: Clock,
): EmbeddedServer<*, *> {
    return embeddedServer(Netty, configure = {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
        shutdownGracePeriod = TimeUnit.SECONDS.toMillis(5)
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
            azureConfig = azureConfig,
        )

        val motor = startMotor(dataSource, repositoryRegistry, prometheus, clock)

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
                    route("api") {
                        ansvarligSystemApi(dataSource, repositoryRegistry, clock)
                        arenaApi()
                        meldeperioderApi(dataSource, MeldeperiodeFlateFactoryImpl(clock), repositoryRegistry)
                        utfyllingApi(dataSource, UtfyllingFlateFactoryImpl(clock), repositoryRegistry)
                        metadataApi(dataSource, repositoryRegistry, clock)
                    }
                }
            }
            authenticate(AZURE) {
                apiRouting {
                    motorApi(dataSource)
                    behandlingsflytApi(dataSource, repositoryRegistry, GatewayProvider, clock)
                }
            }
            actuator(prometheus, motor)
        }
    }.start(wait = wait)
}

private fun Application.startMotor(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    prometheus: PrometheusMeterRegistry,
    clock: Clock,
): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = 4,
        logInfoProvider = JournalføringJobbUtfører.LogInfoProvider,
        jobber = listOf(
            JournalføringJobbUtfører.jobbKonstruktør(repositoryRegistry),
            SlettGamleUtfyllingJobbUtfører.jobbKonstruktør(repositoryRegistry, clock),
            SendVarselJobbUtfører.jobbKonstruktør(repositoryRegistry, clock),
        ),
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

fun OpenAPIPipelineResponseContext<*>.innloggetBruker() =
    InnloggetBruker(
        ident = Ident(personBruker().pid),
        token = token().token(),
    )
