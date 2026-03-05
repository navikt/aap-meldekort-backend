package no.nav.aap.meldekort

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
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

                is VerdiIkkeFunnetException -> {
                    logger.info("Fant ikke verdi. Leverer 404. ${cause.message}", cause)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is ManglerTilgangException -> {
                    logger.warn("Mangler tilgang til å vise route: '{}'", call.request.local.uri, cause)
                    call.respondWithError(IkkeTillattException(message = "Mangler tilgang"))
                }

                is IkkeFunnetException -> {
                    logger.warn("Fikk 404 fra ekstern integrasjon.", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.NotFound,
                            message = "Fikk 404 fra ekstern integrasjon. Dette er mest sannsynlig en systemfeil."
                        )
                    )
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
