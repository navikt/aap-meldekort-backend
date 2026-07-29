package no.nav.aap.meldekort.test

import com.fasterxml.jackson.databind.JsonNode
import com.nimbusds.jwt.JWTParser
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.UUID

object FakeTexas : FakeServer {
    private val log = LoggerFactory.getLogger(this.javaClass)!!

    private val azp = UUID.randomUUID().toString()

    override fun setProperties(port: Int) {
        System.setProperty("nais.token.endpoint", "http://localhost:$port/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:$port/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:$port/introspect")

        System.setProperty("BEHANDLINGSFLYT_AZP", azp)
    }

    override val module: Application.() -> Unit =
        {
            install(ContentNegotiation) {
                jackson()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    log.info("Texas :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ErrorRespons(cause.message)
                    )
                }
            }
            routing {
                post("/token") {
                    val token = TokenGen
                        .generate(
                            issuer = "meldekort-backend",
                            audience = "meldekort-backend",
                            claims = listOfNotNull(
                                "NAVident" to "Lokalsaksbehandler",
                                "azp_name" to "azp",
                                "idtyp" to "app",
                                "azp" to azp
                            )
                        )
                    call.respond(FakeServers.TestToken(access_token = token))
                }

                post("/token/exchange") {
                    val body = call.receive<JsonNode>()
                    val NAVident = JWTParser.parse(body["user_token"].asText())
                        .jwtClaimsSet
                        .getClaimAsString("NAVident")

                    val token = TokenGen.generate(
                        issuer = "meldekort-backend",
                        "meldekort-backend",
                        listOf("NAVIdent" to NAVident)
                    )
                    call.respond(FakeServers.TestToken(access_token = token))
                }

                post("/introspect") {
                    call.respond(mapOf("active" to true))
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
