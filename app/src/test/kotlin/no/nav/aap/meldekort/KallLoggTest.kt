package no.nav.aap.meldekort

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.queryString
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.intercept
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import no.nav.aap.Ident
import no.nav.aap.kallogg.KallLoggStart
import no.nav.aap.kallogg.KallLoggUtfall
import no.nav.aap.komponenter.httpklient.exception.TimeoutException
import no.nav.aap.komponenter.server.auth.IdentityProvider
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KallLoggTest {
    private val now = Instant.parse("2026-02-03T12:34:56Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val algorithm = Algorithm.HMAC256("kallogg-test-key")
    private val token = JWT.create().withClaim("pid", "12345678901").sign(algorithm)
    private val paths = listOf(
        "/api/meldeperiode/kommende",
        "/api/meldeperiode/historiske",
        "/api/ansvarlig-system",
        "/api/v2/ansvarlig-system",
    )

    @Test
    fun `selected GET routes record authenticated identity receipt time repeated query and exact serialized response`() {
        paths.forEach { path ->
            val log = RecordingLog()
            withServer(log) { baseUrl ->
                val response = request("$baseUrl$path?tag=one&tag=two&empty=&escaped=a%2Bb")
                val completed = log.awaitCompletion()

                assertEquals(200, response.statusCode())
                assertEquals(Ident("12345678901"), log.starts.single().ident)
                assertEquals(LocalDate.of(2026, 2, 3), log.starts.single().day)
                val entry = log.starts.single().entry
                assertEquals(now, entry.receivedAt)
                assertEquals("GET", entry.method)
                assertEquals(path, entry.path)
                assertEquals(
                    mapOf("tag" to listOf("one", "two"), "empty" to listOf(""), "escaped" to listOf("a+b")),
                    entry.queryParameters,
                )
                assertEquals(entry.id, completed.id)
                assertEquals(now, completed.at)
                assertEquals(200, completed.status)
                assertEquals(response.body(), completed.body)
                assertEquals(KallLoggUtfall.COMPLETED, completed.outcome)
                assertTrue(response.body().contains("æøå"))
            }
        }
    }

    @Test
    fun `receipt timestamp precedes authentication and serialization runs exactly once`() {
        val currentTime = AtomicReference(now)
        val advancingClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC
            override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
            override fun instant(): Instant = currentTime.get()
        }
        val serializations = AtomicInteger()
        val log = RecordingLog()
        withServer(
            log,
            testClock = advancingClock,
            afterAuthentication = { currentTime.set(now.plusSeconds(60)) },
            serializations = serializations,
        ) { baseUrl ->
            val response = request("$baseUrl${paths.first()}?once")
            val completed = log.awaitCompletion()
            assertEquals(now, log.starts.single().entry.receivedAt)
            assertEquals(now.plusSeconds(60), completed.at)
            assertEquals(1, serializations.get())
            assertEquals(response.body(), completed.body)
        }
    }

    @Test
    fun `percent encoded path to a selected GET is logged`() {
        val log = RecordingLog()
        withServer(log) { baseUrl ->
            val response = request("$baseUrl/api/meldeperiode/%6Bommende")
            assertEquals(200, response.statusCode())
            val completed = log.awaitCompletion()
            assertEquals(log.starts.single().entry.id, completed.id)
            assertEquals(response.body(), completed.body)
        }
    }

    @Test
    fun `unauthenticated requests other routes and other methods are excluded`() {
        val log = RecordingLog()
        withServer(log) { baseUrl ->
            assertEquals(401, request("$baseUrl${paths.first()}", authenticated = false).statusCode())
            assertEquals(200, request("$baseUrl/api/other").statusCode())
            assertEquals(200, request("$baseUrl${paths.first()}", method = "POST").statusCode())
            assertEquals(200, request("$baseUrl/entra${paths.first()}").statusCode())
            assertTrue(log.starts.isEmpty())
            assertTrue(log.completions.isEmpty())
        }
    }

    @Test
    fun `status page errors validation before handler and explicit failures retain actual body and status`() {
        listOf("error", "validation", "forbidden").forEach { scenario ->
            val log = RecordingLog()
            withServer(log) { baseUrl ->
                val response = request("$baseUrl${paths.first()}?$scenario")
                val completed = log.awaitCompletion()
                assertTrue(response.statusCode() >= 400)
                assertEquals(response.statusCode(), completed.status)
                assertEquals(response.body(), completed.body)
                assertEquals(KallLoggUtfall.FAILED, completed.outcome)
                assertEquals(1, log.starts.size)
                assertEquals(1, log.completions.size)
            }
        }
    }

    @Test
    fun `backend timeout records StatusPages response as timed out`() {
        val log = RecordingLog()
        withServer(log) { baseUrl ->
            val response = request("$baseUrl${paths.first()}?timeout")
            val completed = log.awaitCompletion()
            assertEquals(TimeoutException("test").status.value, response.statusCode())
            assertEquals(response.statusCode(), completed.status)
            assertEquals(response.body(), completed.body)
            assertEquals(KallLoggUtfall.TIMED_OUT, completed.outcome)
        }
    }

    @Test
    fun `logging failures never change successful or failed HTTP responses and log no sensitive values`() {
        val logger = LoggerFactory.getLogger(KallLogg::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start(); logger.addAppender(it) }
        try {
            listOf(true, false).forEach { failStart ->
                listOf("", "?error").forEach { scenario ->
                    val log = RecordingLog(failStart = failStart, failComplete = !failStart)
                    withServer(log) { baseUrl ->
                        val response = request("$baseUrl${paths.first()}$scenario")
                        assertEquals(if (scenario.isEmpty()) 200 else 500, response.statusCode())
                        if (!failStart) assertTrue(log.completionAttempt.await(5, TimeUnit.SECONDS))
                        assertFalse(response.body().contains("secret-logging-payload"))
                    }
                }
            }
            assertEquals(4, appender.list.size)
            appender.list.forEach {
                assertTrue(it.formattedMessage.startsWith("HTTP call logging failed during "))
                assertFalse(it.formattedMessage.contains("12345678901"))
                assertFalse(it.formattedMessage.contains("secret-logging-payload"))
                assertNull(it.throwableProxy)
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `cancellation reaches server unchanged and leaves initial row without invented response`() {
        val log = RecordingLog()
        val cancellationPropagated = AtomicBoolean()
        withServer(log, cancellationPropagated) { baseUrl ->
            runCatching { request("$baseUrl${paths.first()}?cancel") }
            assertEquals(1, log.starts.size)
            assertTrue(cancellationPropagated.get())
            assertTrue(log.completions.isEmpty())
        }
    }

    @Test
    fun `missing Kelvin mapping leaves response unchanged without completion`() {
        val log = RecordingLog(noMapping = true)
        withServer(log) { baseUrl ->
            assertEquals(200, request("$baseUrl${paths.first()}").statusCode())
            assertEquals(1, log.starts.size)
            assertTrue(log.completions.isEmpty())
        }
    }

    private fun withServer(
        recordingLog: RecordingLog,
        cancellationPropagated: AtomicBoolean = AtomicBoolean(),
        testClock: Clock = clock,
        afterAuthentication: () -> Unit = {},
        serializations: AtomicInteger = AtomicInteger(),
        test: (String) -> Unit,
    ) {
        val cancellation = CancellationException("business cancellation")
        val server = embeddedServer(Netty, port = 0) {
            install(ContentNegotiation) { jackson() }
            intercept(ApplicationCallPipeline.Monitoring) {
                try {
                    proceed()
                } catch (cause: CancellationException) {
                    // Coroutine stack-trace recovery may copy the exception and retain the original as its cause.
                    cancellationPropagated.set(
                        generateSequence<Throwable>(cause) { it.cause }.take(16).any { it === cancellation }
                    )
                    throw cause
                }
            }
            install(StatusPages, StatusPagesConfigHelper.setup())
            authentication {
                jwt(IdentityProvider.TOKENX.value) {
                    verifier(JWT.require(algorithm).build())
                    validate {
                        afterAuthentication()
                        JWTPrincipal(it.payload)
                    }
                }
            }
            val logging = KallLogg(testClock, recordingLog::start, recordingLog::complete)
            logging.install(this)
            routing {
                authenticate(IdentityProvider.TOKENX.value) {
                    paths.forEach { path ->
                        route(path) {
                            logging.installAuthenticatedGet(this)
                            intercept(ApplicationCallPipeline.Call) {
                                if (call.request.queryString() == "validation") {
                                    throw BadRequestException("Invalid query parameter")
                                }
                            }
                            get {
                                when (call.request.queryString()) {
                                    "error" -> error("business failure")
                                    "timeout" -> throw HttpConnectTimeoutException("backend timed out")
                                    "cancel" -> throw cancellation
                                    "once" -> call.respond(CountingBody(serializations))
                                    "forbidden" -> call.respondText("denied", status = HttpStatusCode.Forbidden)
                                    else -> call.respond(mapOf("text" to "æøå\n\"quoted\"", "values" to listOf(1, 2)))
                                }
                            }
                            post { call.respondText("not logged") }
                        }
                    }
                    get("/api/other") { call.respondText("not logged") }
                }
                get("/entra${paths.first()}") { call.respondText("not logged", ContentType.Text.Plain) }
            }
        }.start(wait = false)
        try {
            val port = runBlocking { server.engine.resolvedConnectors().single().port }
            test("http://localhost:$port")
        } finally {
            server.stop(0, 1000)
        }
    }

    private fun request(url: String, authenticated: Boolean = true, method: String = "GET"): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(2))
            .method(method, HttpRequest.BodyPublishers.noBody())
        if (authenticated) request.header("Authorization", "Bearer $token")
        return HttpClient.newHttpClient().use { client ->
            client.send(request.build(), HttpResponse.BodyHandlers.ofString())
        }
    }

    private class RecordingLog(
        private val failStart: Boolean = false,
        private val failComplete: Boolean = false,
        private val noMapping: Boolean = false,
    ) {
        val starts = CopyOnWriteArrayList<Started>()
        val completions = CopyOnWriteArrayList<Completed>()
        val completionAttempt = CountDownLatch(1)

        fun start(ident: Ident, day: LocalDate, entry: KallLoggStart): UUID? {
            if (failStart) error("secret-logging-payload 12345678901")
            starts.add(Started(ident, day, entry))
            return entry.id.takeUnless { noMapping }
        }

        fun complete(id: UUID, at: Instant, status: Int?, body: String?, outcome: KallLoggUtfall) {
            try {
                if (failComplete) error("secret-logging-payload 12345678901")
                completions.add(Completed(id, at, status, body, outcome))
            } finally {
                completionAttempt.countDown()
            }
        }

        fun awaitCompletion(): Completed {
            assertTrue(completionAttempt.await(5, TimeUnit.SECONDS), "Expected call log completion")
            return completions.single()
        }
    }

    private data class Started(val ident: Ident, val day: LocalDate, val entry: KallLoggStart)
    private class CountingBody(private val serializations: AtomicInteger) {
        val value: Int get() = serializations.incrementAndGet()
    }

    private data class Completed(
        val id: UUID,
        val at: Instant,
        val status: Int?,
        val body: String?,
        val outcome: KallLoggUtfall,
    )
}
