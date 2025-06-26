package no.nav.aap.meldekort.test

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.util.UUID

object FakeAzure : FakeServer {
    val log = LoggerFactory.getLogger(this.javaClass)!!
    private val azp = UUID.randomUUID().toString()

    override fun setProperties(port: Int) {
        println("AZURE PORT $port")
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:$port/token")
        System.setProperty("azure.app.client.id", "meldekort-backend")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:$port/jwks")
        System.setProperty("azure.openid.config.issuer", "fake-azure")
        System.setProperty("BEHANDLINGSFLYT_AZP", azp)
    }

    override val module: Application.() -> Unit =
        {
            install(ContentNegotiation) {
                jackson()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    log.info("TokenX :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(
                        status = HttpStatusCode.Companion.InternalServerError,
                        message = ErrorRespons(cause.message)
                    )
                }
            }
            routing {
                post("/token") {
                    log.info("Henter fake Azure-token")
                    val body = call.receiveText()
                    val token = TokenGen.generate(
                        issuer = "fake-azure",
                        audience = "meldekort-backend",
                        listOfNotNull(
                            "NAVident" to "Lokalsaksbehandler",
                            "azp_name" to "azp",
                            if (body.contains("grant_type=client_credentials")) {
                                "idtyp" to "app"
                            } else {
                                null
                            },
                            "azp" to azp
                        )
                    )
                    call.respond(TestToken(access_token = token))
                }
                get("/jwks") {
                    call.respond(LOCAL_JWKS)
                }
            }
        }
}
