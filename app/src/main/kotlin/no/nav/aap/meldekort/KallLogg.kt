package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.http.HttpMethod
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutputStreamContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import no.nav.aap.Ident
import no.nav.aap.kallogg.KallLoggRepository
import no.nav.aap.kallogg.KallLoggStart
import no.nav.aap.kallogg.KallLoggUtfall
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.TimeoutException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.personBruker
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.net.http.HttpTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

internal class KallLogg(
    private val clock: Clock,
    private val start: (Ident, LocalDate, KallLoggStart) -> UUID?,
    private val fullført: (UUID, Instant, Int?, String?, KallLoggUtfall) -> Unit,
) {
    constructor(dataSource: DataSource, repositoryRegistry: RepositoryRegistry, clock: Clock) : this(
        clock,
        start = { ident, day, entry ->
            dataSource.transaction { connection ->
                repositoryRegistry.provider(connection).provide<KallLoggRepository>().start(ident, day, entry)
            }
        },
        fullført = { id, completedAt, status, body, outcome ->
            dataSource.transaction { connection ->
                repositoryRegistry.provider(connection).provide<KallLoggRepository>()
                    .complete(id, completedAt, status, body, outcome)
            }
        },
    )

    fun install(application: Application) {
        application.intercept(ApplicationCallPipeline.Setup) {
            call.attributes.put(receivedAtKey, clock.instant())
        }

        // After serialization, but before compression or transfer encoding can wrap the content.
        val serializedResponse = PipelinePhase("KallLoggSerialisertRespons")
        application.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, serializedResponse)
        application.sendPipeline.intercept(serializedResponse) {
            val entry = call.attributes.getOrNull(entryKey) ?: return@intercept
            // The engine may send a fallback response after cancellation; it is not a completed application call.
            if (entry.cancelled) return@intercept
            val capture = ResponseCapture()
            val content = try {
                (subject as? OutgoingContent)?.let(capture::observe)
            } catch (_: Exception) {
                logger.error("HTTP call logging failed during response capture")
                null
            }

            // A failed/cancelled send has no known final response: retain the initial IN_PROGRESS row.
            if (content == null) proceed() else proceedWith(content)
            if (call.response.isCommitted) {
                isolated("fullført") {
                    val status = call.response.status()?.value
                    fullført(
                        entry.id,
                        clock.instant(),
                        status,
                        capture.body(),
                        when {
                            entry.timedOut -> KallLoggUtfall.TIMED_OUT
                            status != null && status >= 400 -> KallLoggUtfall.FAILED
                            else -> KallLoggUtfall.COMPLETED
                        },
                    )
                }
            }
        }
    }

    fun installAuthenticatedGet(route: Route) {
        val getRoute = route.createChild(HttpMethodRouteSelector(HttpMethod.Get))
        getRoute.install(createRouteScopedPlugin("KallLoggAutentisering") {
            on(AuthenticationChecked) { call ->
                if (call.principal<JWTPrincipal>() == null || call.attributes.contains(entryKey)) return@on
                isolated("start") {
                    val receivedAt = call.attributes[receivedAtKey]
                    val entry = KallLoggStart(
                        id = UUID.randomUUID(),
                        receivedAt = receivedAt,
                        method = call.request.httpMethod.value,
                        path = call.request.path(),
                        queryParameters = call.request.queryParameters.entries().associate { (name, values) ->
                            name to values.toList()
                        },
                    )
                    start(Ident(call.personBruker().pid), receivedAt.atZone(clock.zone).toLocalDate(), entry)
                        ?.let { call.attributes.put(entryKey, Entry(it)) }
                }
            }
            on(CallFailed) { call, cause ->
                call.attributes.getOrNull(entryKey)?.let { entry ->
                    entry.cancelled = cause is CancellationException
                    entry.timedOut = generateSequence(cause) { it.cause }.take(16).any {
                        it is TimeoutException || it is HttpTimeoutException || it is TimeoutCancellationException ||
                                it is java.net.SocketTimeoutException || it is java.util.concurrent.TimeoutException
                    }
                }
                throw cause
            }
        })
    }

    private class ResponseCapture {
        private var body: () -> String? = { null }

        fun body(): String? = body.invoke()

        @OptIn(io.ktor.utils.io.InternalAPI::class)
        fun observe(content: OutgoingContent): OutgoingContent {
            val charset = content.contentType?.charset() ?: Charsets.UTF_8
            return when (content) {
                is OutgoingContent.ByteArrayContent -> {
                    val bytes = content.bytes()
                    body = { bytes.toString(charset) }
                    content
                }

                is OutgoingContent.NoContent -> {
                    body = { "" }
                    content
                }

                is OutgoingContent.ContentWrapper -> content.copy(observe(content.delegate()))
                is OutputStreamContent -> {
                    val buffer = ByteArrayOutputStream()
                    body = { buffer.toString(charset) }
                    val tee = OutputStreamContent(
                        body = {
                            // Jackson streams its serialized bytes. Observe that single serialization as it is sent.
                            content.writeTo(object : FilterOutputStream(this) {
                                override fun write(value: Int) {
                                    out.write(value)
                                    buffer.write(value)
                                }

                                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                                    out.write(bytes, offset, length)
                                    buffer.write(bytes, offset, length)
                                }
                            })
                        },
                        contentType = content.contentType,
                        status = content.status,
                        contentLength = content.contentLength,
                    )
                    MetadataContent(tee, content)
                }

                else -> {
                    // Never drain an unknown channel or attempt a second serialization.
                    logger.error("HTTP call logging cannot capture this response content type")
                    content
                }
            }
        }
    }

    private class MetadataContent(delegate: OutgoingContent, private val original: OutgoingContent) :
        OutgoingContent.ContentWrapper(delegate) {
        override val headers get() = original.headers
        override fun <T : Any> getProperty(key: AttributeKey<T>): T? = original.getProperty(key)
        override fun <T : Any> setProperty(key: AttributeKey<T>, value: T?) = original.setProperty(key, value)
        override fun trailers() = original.trailers()
        override fun copy(delegate: OutgoingContent) = MetadataContent(delegate, original)
    }

    private suspend fun isolated(operation: String, block: () -> Unit) {
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (_: Exception) {
                    logger.error("HTTP call logging failed during {}", operation)
                }
            }
        }
    }

    private class Entry(val id: UUID, var timedOut: Boolean = false, var cancelled: Boolean = false)

    private companion object {
        val logger = LoggerFactory.getLogger(KallLogg::class.java)
        val receivedAtKey = AttributeKey<Instant>("kallogg-mottatt")
        val entryKey = AttributeKey<Entry>("kallogg-oppføring")
    }
}

internal fun NormalOpenAPIRoute.medKallLogg(kallLogg: KallLogg): NormalOpenAPIRoute =
    apply { kallLogg.installAuthenticatedGet(ktorRoute) }
