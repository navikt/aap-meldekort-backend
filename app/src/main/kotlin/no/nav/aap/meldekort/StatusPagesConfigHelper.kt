package no.nav.aap.meldekort

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import org.slf4j.LoggerFactory

object StatusPagesConfigHelper {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is ManglerTilgangException -> {
                    logger.warn("Mangler tilgang til å vise route: '{}'", call.request.local.uri, cause)
                    call.respondWithError(IkkeTillattException(message = "Mangler tilgang"))
                }

                else -> {
                    logger.error("Ukjent feil ved kall til '${call.request.local.uri}': ", cause)
                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }
    }

    private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}
