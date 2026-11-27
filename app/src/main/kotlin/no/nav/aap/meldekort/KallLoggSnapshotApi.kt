package no.nav.aap.meldekort

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.status
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.ApplicationReceivePipeline
import io.ktor.server.request.contentType
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import no.nav.aap.Ident
import no.nav.aap.kallogg.KallLoggRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.personBruker
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import java.nio.charset.CharacterCodingException
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import javax.sql.DataSource

private const val VARSEL_BYTES = 3 * 1024
private const val MAKS_BYTES = 4 * 1024
private val snapshotStørrelseNøkkel = AttributeKey<Int>("snapshot-body-bytes")
private val snapshotMottattNøkkel = AttributeKey<Instant>("snapshot-received")
private val log = LoggerFactory.getLogger("no.nav.aap.meldekort.KallLoggSnapshotApi")
private val snapshotReader = DefaultJsonMapper.objectMapper().reader()
    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
private val tillatteHtmlElementer = Safelist.none().addTags(
    "article", "section", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6", "p", "br",
    "ul", "ol", "li", "dl", "dt", "dd", "strong", "em", "b", "i", "table", "caption",
    "thead", "tbody", "tfoot", "tr", "th", "td",
)

internal data class KallLoggSnapshotRequest(val html: String)

internal class SnapshotRequestException(val status: HttpStatusCode, val beskrivelse: String) : RuntimeException(beskrivelse)

private object FørSnapshotDeserialisering : Hook<suspend (ApplicationCall, ByteReadChannel) -> KallLoggSnapshotRequest> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall, ByteReadChannel) -> KallLoggSnapshotRequest,
    ) {
        pipeline.receivePipeline.intercept(ApplicationReceivePipeline.Before) {
            val channel = subject as? ByteReadChannel ?: return@intercept
            proceedWith(handler(call, channel))
        }
    }
}

internal fun NormalOpenAPIRoute.kallLoggSnapshotApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry, clock: Clock) {
    kallLoggSnapshotApi(clock) { ident, mottatt, html ->
        withContext(Dispatchers.IO) {
            dataSource.transaction { connection ->
                repositoryRegistry.provider(connection).provide<KallLoggRepository>()
                    .lagreSnapshot(ident, mottatt.atZone(clock.zone).toLocalDate(), mottatt, html)
            }
        }
    }
}

internal fun NormalOpenAPIRoute.kallLoggSnapshotApi(
    clock: Clock,
    lagreSnapshot: suspend (Ident, Instant, String) -> Unit,
) {
    route("call-log") {
        ktorRoute.install(createRouteScopedPlugin("KallLoggSnapshotBody") {
            onCall { call -> call.attributes.put(snapshotMottattNøkkel, clock.instant()) }
            on(FørSnapshotDeserialisering) { call, channel ->
                val encodings = call.request.headers.getAll(HttpHeaders.ContentEncoding)
                if (!call.request.contentType().match(ContentType.Application.Json) ||
                    (encodings != null && !encodings.singleOrNull().equals("identity", ignoreCase = true))
                ) {
                    throw SnapshotRequestException(HttpStatusCode.UnsupportedMediaType, "Ugyldig forespørsel")
                }
                val bytes = channel.readBuffer(MAKS_BYTES.toLong() + 1).use { it.readByteArray() }
                if (bytes.size > MAKS_BYTES) {
                    throw SnapshotRequestException(
                        HttpStatusCode.PayloadTooLarge,
                        "Forespørselen kan ikke overstige $MAKS_BYTES byte.",
                    )
                }
                val html = lesSnapshot(bytes) ?: throw SnapshotRequestException(
                    HttpStatusCode.BadRequest,
                    "Tom eller ugyldig request body",
                )
                call.attributes.put(snapshotStørrelseNøkkel, bytes.size)
                KallLoggSnapshotRequest(html)
            }
        })
        post<Unit, Unit, KallLoggSnapshotRequest>(status(HttpStatusCode.NoContent)) { _, body ->
            val call = pipeline.call
            val rensetHtml = Jsoup.clean(body.html, tillatteHtmlElementer)
            if (rensetHtml.isBlank()) {
                log.info("Renset HTML er tom")
                call.respondWithSnapshotError(HttpStatusCode.BadRequest, "HTML-innholdet er tomt etter rensing.")
                return@post
            }
            val størrelse = call.attributes[snapshotStørrelseNøkkel]
            if (størrelse > VARSEL_BYTES) {
                log.warn("Snapshot nærmer seg øvre grense: {} bytes i request body", størrelse)
            }
            try {
                lagreSnapshot(Ident(call.personBruker().pid), call.attributes[snapshotMottattNøkkel], rensetHtml)
            } catch (_: SQLException) {
                log.error("Kunne ikke lagre snapshot fra frontend")
                call.respondWithSnapshotError(HttpStatusCode.InternalServerError, "Kunne ikke lagre HTML-innholdet.")
                return@post
            }
            respondWithStatus(HttpStatusCode.NoContent)
        }
    }
}

private fun lesSnapshot(body: ByteArray): String? {
    val json: JsonNode? = try {
        snapshotReader.readTree(body.decodeToString(throwOnInvalidSequence = true))
    } catch (_: CharacterCodingException) {
        return null
    } catch (_: JsonProcessingException) {
        return null
    }
    return json?.takeIf { it.isObject }?.get("html")?.takeIf { it.isTextual }
        ?.textValue()?.takeUnless { it.isBlank() || '\u0000' in it }
}

internal suspend fun ApplicationCall.respondWithSnapshotError(status: HttpStatusCode, beskrivelse: String) {
    respondText(
        DefaultJsonMapper.toJson(mapOf("type" to "about:blank", "title" to status.description, "status" to status.value, "detail" to beskrivelse)),
        ContentType.Application.ProblemJson,
        status,
    )
}
