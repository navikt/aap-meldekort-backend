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

object FakeTokenX : FakeServer {
    val log = LoggerFactory.getLogger(this.javaClass)!!

    override fun setProperties(port: Int) {
        println("TokenX PORT $port")
        System.setProperty("token.x.token.endpoint", "http://localhost:$port/token")
        System.setProperty("token.x.client.id", "meldekort-backend")
        System.setProperty("token.x.private.jwk", "")
        System.setProperty("token.x.jwks.uri", "http://localhost:$port/jwks")
        System.setProperty("token.x.issuer", "fake-tokendings")
        System.setProperty("nais.token.exchange.endpoint", "")
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

@Suppress("PropertyName")
data class TestToken(
    val access_token: String,
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)
