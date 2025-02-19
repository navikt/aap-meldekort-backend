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
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.utfyllingApi(dataSource: DataSource) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(body: UtfyllingService.() -> T): T {
        return dataSource.transaction { connection ->
            UtfyllingService.konstruer(innloggetBruker(), connection).run {
                body()
            }
        }
    }

    route("start-innsending").post<Unit, StartUtfyllingResponse, StartUtfyllingRequest> { params, body ->
        val response = try {
            val utfylling = medFlate {
                startUtfylling(innloggetBruker(), Periode(body.fom, body.tom))
            }
            StartUtfyllingResponse(
                metadata = UtfyllingMetadataDto(utfylling),
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
                metadata = UtfyllingMetadataDto(utfylling),
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
        get<Referanse, HentUtfyllingResponse> { params ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val utfylling = medFlate {
                hent(innloggetBruker(), utfyllingReferanse)
            }
            if (utfylling == null) {
                respondWithStatus(HttpStatusCode.NotFound)

            } else {
                respond(
                    HentUtfyllingResponse(
                        metadata = UtfyllingMetadataDto(utfylling),
                        tilstand = UtfyllingTilstandDto(utfylling),
                    )
                )
            }
        }

        delete<Referanse, Unit> { params ->
            medFlate {
                slett(innloggetBruker(), UtfyllingReferanse(params.referanse))
            }
            respondWithStatus(HttpStatusCode.OK)
        }

        route("lagre-neste").post<Referanse, EndreUtfyllingResponse, EndreUtfyllingRequest> { params, body ->
            val response = medFlate {
                nesteOgLagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = UtfyllingReferanse(params.referanse),
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
            }
            respond(
                EndreUtfyllingResponse(
                    metadata = UtfyllingMetadataDto(response.utfylling),
                    utfyllingTilstand = UtfyllingTilstandDto(response.utfylling),
                    feil = response.feil?.toString() /* TODO */
                )
            )
        }

        route("lagre").post<Referanse, EndreUtfyllingResponse, EndreUtfyllingRequest> { params, body ->
            val response = medFlate {
                lagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = UtfyllingReferanse(params.referanse),
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
            }
            respond(
                EndreUtfyllingResponse(
                    metadata = UtfyllingMetadataDto(response.utfylling),
                    utfyllingTilstand = UtfyllingTilstandDto(response.utfylling),
                    feil = response.feil?.toString() /* TODO */
                )
            )
        }
    }
}