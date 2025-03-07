package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.delete
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingService
import org.slf4j.MDC
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.utfyllingApi(dataSource: DataSource) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(
        utfyllingReferanse: UtfyllingReferanse? = null,
        body: UtfyllingService.() -> T,
    ): T {
        return MDC.putCloseable("utfylling", utfyllingReferanse?.asUuid?.toString()).use {
            dataSource.transaction { connection ->
                UtfyllingService.konstruer(innloggetBruker(), connection).run {
                    body()
                }
            }
        }
    }

    route("start-innsending").post<Unit, StartUtfyllingResponse, StartUtfyllingRequest> { params, body ->
        val response = try {
            val utfylling = medFlate {
                startUtfylling(innloggetBruker(), Periode(body.fom, body.tom))
            }
            StartUtfyllingResponse(
                metadata = UtfyllingMetadataDto.fraDomene(utfylling),
                tilstand = UtfyllingTilstandDto(utfylling),
                feil = null
            )
        } catch (exception: Exception) {
            StartUtfyllingResponse(null, null, exception.javaClass.canonicalName) /* TODO */
        }
        respond(response)
    }
    route("start-korrigering").post<Unit, StartUtfyllingResponse, StartUtfyllingRequest> { params, body ->
        val response = try {
            val utfylling = medFlate {
                startKorrigering(innloggetBruker(), Periode(body.fom, body.tom))
            }
            StartUtfyllingResponse(
                metadata = UtfyllingMetadataDto.fraDomene(utfylling),
                tilstand = UtfyllingTilstandDto(utfylling),
                feil = null
            )
        } catch (exception: Exception) {
            StartUtfyllingResponse(null, null, exception.javaClass.canonicalName) /* TODO */
        }
        respond(response)
    }

    route("utfylling/{referanse}") {
        class Referanse(
            @PathParam("referanse") val referanse: UUID,
        )
        get<Referanse, UtfyllingResponseDto> { params ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val utfylling = medFlate(utfyllingReferanse) {
                hent(innloggetBruker(), utfyllingReferanse)
            }
            if (utfylling == null) {
                respondWithStatus(HttpStatusCode.NotFound)

            } else {
                respond(
                    UtfyllingResponseDto(
                        metadata = UtfyllingMetadataDto.fraDomene(utfylling),
                        tilstand = UtfyllingTilstandDto(utfylling),
                        feil = null,
                    )
                )
            }
        }

        delete<Referanse, Unit> { params ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            medFlate(utfyllingReferanse) {
                slett(innloggetBruker(), utfyllingReferanse)
            }
            respondWithStatus(HttpStatusCode.OK)
        }

        route("lagre-neste").post<Referanse, UtfyllingResponseDto, EndreUtfyllingRequest> { params, body ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val response = medFlate(utfyllingReferanse) {
                nesteOgLagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = utfyllingReferanse,
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
            }
            respond(
                UtfyllingResponseDto(
                    metadata = UtfyllingMetadataDto.fraDomene(response.utfylling),
                    tilstand = UtfyllingTilstandDto(response.utfylling),
                    feil = response.feil?.toString() /* TODO */
                )
            )
        }

        route("lagre").post<Referanse, UtfyllingResponseDto, EndreUtfyllingRequest> { params, body ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val response = medFlate(utfyllingReferanse) {
                lagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = utfyllingReferanse,
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
            }
            respond(
                UtfyllingResponseDto(
                    metadata = UtfyllingMetadataDto.fraDomene(response.utfylling),
                    tilstand = UtfyllingTilstandDto(response.utfylling),
                    feil = response.feil?.toString() /* TODO */
                )
            )
        }
    }
}