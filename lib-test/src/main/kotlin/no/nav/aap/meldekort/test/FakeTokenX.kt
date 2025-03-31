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

object FakeTokenX : FakeServer {

    override var port = 8081 /* Så kan frontend hente token lokalt */

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

    override val module: Application.() -> Unit = {
        for (fnr in listOf("1".repeat(11), "2".repeat(11), "3".repeat(11))) {
            println("FAKE TOKENX-TOKEN fnr $fnr: ${issueToken(fnr)}")
        }

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
                val token = issueToken("1".repeat(11))
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(LOCAL_JWKS)
            }
        }
    }

    fun issueToken(fnr: String): String {
        return TokenGen.generate(
            issuer = "fake-tokendings",
            audience = "meldekort-backend",
            listOf("pid" to fnr)
        )
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
