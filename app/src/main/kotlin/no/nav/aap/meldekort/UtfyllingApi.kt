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
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingFlateFactory
import no.nav.aap.utfylling.UtfyllingReferanse
import org.slf4j.MDC
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.utfyllingApi(
    dataSource: DataSource,
    utfyllingFlateFactory: UtfyllingFlateFactory,
    repositoryRegistry: RepositoryRegistry,
) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(
        utfyllingReferanse: UtfyllingReferanse? = null,
        body: UtfyllingFlate.() -> T,
    ): T {
        return MDC.putCloseable("utfylling", utfyllingReferanse?.asUuid?.toString()).use {
            dataSource.transaction { connection ->
                val flate = utfyllingFlateFactory.flateForBruker(
                    innloggetBruker(),
                    repositoryProvider = repositoryRegistry.provider(connection),
                    gatewayProvider = GatewayProvider,
                    connection = connection,
                )
                flate.body()
            }
        }
    }

    route("start-innsending").post<Unit, StartUtfyllingResponse, StartUtfyllingRequest> { _, body ->
        val response = try {
            medFlate {
                val resultat = startUtfylling(innloggetBruker(), Periode(body.fom, body.tom))
                StartUtfyllingResponse.fraDomene(resultat)
            }
        } catch (exception: Exception) {
            StartUtfyllingResponse(null, null, exception.javaClass.canonicalName) /* TODO */
        }
        respond(response)
    }

    route("start-korrigering").post<Unit, StartUtfyllingResponse, StartUtfyllingRequest> { _, body ->
        val response = try {
            medFlate {
                val resultat = startKorrigering(innloggetBruker(), Periode(body.fom, body.tom))
                StartUtfyllingResponse.fraDomene(resultat)
            }
        } catch (exception: Exception) {
            StartUtfyllingResponse(null, null, exception.javaClass.canonicalName) /* TODO */
        }
        respond(response)
    }

    route("utfylling/{referanse}") {
        data class Referanse(
            @PathParam("referanse") val referanse: UUID,
        )
        get<Referanse, UtfyllingResponseDto> { params ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val response = medFlate(utfyllingReferanse) {
                val utfylling = hentUtfylling(innloggetBruker(), utfyllingReferanse)
                if (utfylling == null) {
                    return@medFlate null
                } else {
                    UtfyllingResponseDto.fraDomene(utfylling)
                }
            }
            if (response == null)
                respondWithStatus(HttpStatusCode.NotFound)
            else
                respond(response)
        }

        delete<Referanse, Unit> { params ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            medFlate(utfyllingReferanse) {
                slettUtfylling(innloggetBruker(), utfyllingReferanse)
            }
            respondWithStatus(HttpStatusCode.OK)
        }

        route("lagre-neste").post<Referanse, UtfyllingResponseDto, EndreUtfyllingRequest> { params, body ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val response = medFlate(utfyllingReferanse) {
                val utfylling = nesteOgLagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = utfyllingReferanse,
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
                UtfyllingResponseDto.fraDomene(utfylling)
            }
            respond(response)
        }

        route("lagre").post<Referanse, UtfyllingResponseDto, EndreUtfyllingRequest> { params, body ->
            val utfyllingReferanse = UtfyllingReferanse(params.referanse)
            val response = medFlate(utfyllingReferanse) {
                val utfylling = lagre(
                    innloggetBruker = innloggetBruker(),
                    utfyllingReferanse = utfyllingReferanse,
                    aktivtSteg = body.nyTilstand.aktivtSteg.tilDomene,
                    svar = body.nyTilstand.svar.tilDomene(),
                )
                UtfyllingResponseDto.fraDomene(utfylling)
            }
            respond(response)
        }
    }
}