package no.nav.aap.meldekort.test

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

object FakeAzure : FakeServer {
    val log = LoggerFactory.getLogger(this.javaClass)!!

    override fun setProperties(port: Int) {
        println("AZURE PORT $port")
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:$port/token")
        System.setProperty("azure.app.client.id", "meldekort-backend")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:$port/jwks")
        System.setProperty("azure.openid.config.issuer", "fake-azure")
    }

    override val module: Application.() -> Unit =
        {
            install(ContentNegotiation) {
                jackson()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    FakeTokenX.log.info("TokenX :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(
                        status = HttpStatusCode.Companion.InternalServerError,
                        message = ErrorRespons(cause.message)
                    )
                }
            }
            routing {
                post("/token") {
                    val body = call.receiveText()
                    val token = AzureTokenGen(
                        issuer = "meldekort-backend",
                        audience = "meldekort-backend"
                    ).generate(body.contains("grant_type=client_credentials"))
                    call.respond(TestToken(access_token = token))
                }
                get("/jwks") {
                    call.respond(AZURE_JWKS)
                }
            }
        }
}
