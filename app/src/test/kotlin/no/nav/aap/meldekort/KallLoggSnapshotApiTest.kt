package no.nav.aap.meldekort

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.Ident
import no.nav.aap.komponenter.server.auth.IdentityProvider
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KallLoggSnapshotApiTest {
    private val mottattTidspunkt = Instant.parse("2026-02-03T12:34:56Z")
    private val clock = Clock.fixed(mottattTidspunkt, ZoneOffset.UTC)
    private val algoritme = Algorithm.HMAC256("snapshot-test-key")
    private val token = JWT.create().withClaim("pid", "12345678901").sign(algoritme)

    @Test
    fun `logger en advarsel når vi nærmer oss maks størrelse på content`() {
        val lagredeSnapshotter = CopyOnWriteArrayList<String>()
        val log = LoggerFactory.getLogger("no.nav.aap.meldekort.KallLoggSnapshotApi") as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start(); log.addAppender(it) }
        try {
            medServer({ _, _, html -> lagredeSnapshotter.add(html) }) { url ->
                listOf(3072, 3073, 4096, 4097).forEach { størrelse ->
                    val body = "{\"html\":\"<p>\u00f8</p>\"}"
                    val bytes = (body + " ".repeat(størrelse - body.toByteArray().size)).toByteArray()
                    val publisher = HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    val response = post(url, publisher)
                    assertEquals(if (størrelse <= 4096) 204 else 413, response.statusCode(), "Størrelse på body: $størrelse")
                }
                assertEquals(413, post(url, HttpRequest.BodyPublishers.ofString("x".repeat(4097))).statusCode())
            }
            assertEquals(List(3) { "<p>\u00f8</p>" }, lagredeSnapshotter)
            assertEquals(
                listOf(
                    "Snapshot nærmer seg øvre grense: 3073 bytes i request body",
                    "Snapshot nærmer seg øvre grense: 4096 bytes i request body",
                ),
                appender.list.filter { it.level == Level.WARN }.map { it.formattedMessage },
            )
        } finally {
            log.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `kall fra ikke-autentiserte brukere blir ignorert`() {
        val lagredeSnapshotter = CopyOnWriteArrayList<Triple<Ident, Instant, String>>()
        val body = HttpRequest.BodyPublishers.ofString(
            """{"html":"<p class='text'>Read <a href='https://example.org'>more</a></p>"}""",
        )
        medServer({ ident, mottatt, html -> lagredeSnapshotter.add(Triple(ident, mottatt, html)) }) { url ->
            assertEquals(401, post(url, body, authenticated = false).statusCode())
            assertEquals(400, post(url, HttpRequest.BodyPublishers.ofString("""{"html":42}""")).statusCode())
            assertTrue(lagredeSnapshotter.isEmpty())
            assertEquals(204, post(url, body).statusCode())
        }
        assertEquals(listOf(Triple(Ident("12345678901"), mottattTidspunkt, "<p>Read more</p>")), lagredeSnapshotter)
    }

    @Test
    fun `ugyldig json gir bad request`() {
        medServer({ _, _, _ -> error("Uventet lagring") }) { url ->
            listOf(
                """{"html":""}""",
                """{"html":"first","html":"second"}""",
                """{"html":"Text"} {"html":"extra"}""",
                """{"html":""",
            ).forEach { body ->
                assertEquals(400, post(url, HttpRequest.BodyPublishers.ofString(body)).statusCode())
            }
        }
    }

    private fun medServer(lagreSnapshot: suspend (Ident, Instant, String) -> Unit, test: (URI) -> Unit) {
        val server = embeddedServer(Netty, port = 0) {
            install(ContentNegotiation) { jackson() }
            install(OpenAPIGen) { serveSwaggerUi = false }
            install(StatusPages, StatusPagesConfigHelper.setup())
            authentication {
                jwt(IdentityProvider.TOKENX.value) {
                    verifier(JWT.require(algoritme).build())
                    validate { JWTPrincipal(it.payload) }
                }
            }
            routing {
                authenticate(IdentityProvider.TOKENX.value) {
                    apiRouting {
                        route("api") {
                            kallLoggSnapshotApi(clock, lagreSnapshot)
                        }
                    }
                }
            }
        }.start(wait = false)
        try {
            val port = runBlocking { server.engine.resolvedConnectors().single().port }
            test(URI.create("http://localhost:$port/api/call-log"))
        } finally {
            server.stop(0, 1000)
        }
    }

    private fun post(url: URI, body: HttpRequest.BodyPublisher, authenticated: Boolean = true): HttpResponse<String> {
        val request = HttpRequest.newBuilder(url).header("Content-Type", "application/json").POST(body)
        if (authenticated) request.header("Authorization", "Bearer $token")
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString())
    }
}
