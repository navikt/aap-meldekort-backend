package no.nav.aap.meldekort.arena

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.httpklient.auth.personBruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker

fun NormalOpenAPIRoute.meldekortApi(
    arenaSkjemaFlate: ArenaSkjemaFlate,
    arenaClient: ArenaClient,
) {
    route("/api/arena") {
        class MeldekortIdParam(
            @JsonValue @PathParam("meldekortId") val meldekortId: Long,
        )
        route("/skjema/{meldekortId}") {
            get<MeldekortIdParam, MeldekortResponse> { params ->
                val nåværendeTilstand = arenaSkjemaFlate.hentEllerOpprettSkjema(innloggetBruker(), params.meldekortId)
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<MeldekortIdParam, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                respond(
                    try {
                        MeldekortResponse(
                            arenaSkjemaFlate.gåTilNesteSteg(
                                innloggetBruker = innloggetBruker(),
                                meldekortId = params.meldekortId,
                                fraSteg = meldekortRequest.nåværendeSteg,
                                nyPayload = meldekortRequest.meldekort.tilDomene(),
                            )
                        )
                    } catch (e: ArenaInnsendingFeiletException) {
                        MeldekortResponse(
                            skjema = e.skjema!!,
                            feil = InnsendingFeil(e.innsendingFeil)
                        )
                    }
                )
            }

            route("/lagre").post<MeldekortIdParam, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        arenaSkjemaFlate.lagreSteg(
                            ident = innloggetBruker().ident,
                            meldekortId = params.meldekortId,
                            nyPayload = meldekortRequest.meldekort.tilDomene(),
                            settSteg = meldekortRequest.nåværendeSteg,
                        )
                    )
                )
            }
        }

        route("meldekort") {
            route("/neste").get<Unit, KommendeMeldekortDto> {
                val kommendeMeldekort = arenaSkjemaFlate.kommendeMeldekort(innloggetBruker())
                respond(
                    KommendeMeldekortDto(
                        antallUbesvarteMeldekort = kommendeMeldekort.size,
                        nesteMeldekort = kommendeMeldekort.minByOrNull { it.periode }?.let {
                            NesteMeldekortDto(
                                meldeperiode = PeriodeDto(it.periode),
                                meldekortId = it.meldekortId,
                                tidligsteInnsendingsDato = it.tidligsteInnsendingsdato,
                                kanSendesInn = it.kanSendes
                            )
                        }
                    )
                )
            }

            route("/historisk").get<Unit, List<HistoriskMeldekortDto>> {
                respond(
                    arenaSkjemaFlate.historiskeMeldekort(
                        innloggetBruker()
                    ).map {
                        HistoriskMeldekortDto(
                            meldeperiode = PeriodeDto(it.periode),
                            meldekortId = it.meldekortId,
                            status = it.beregningStatus
                        )
                    }
                )
            }

            route("/historisk/{meldekortId}").get<MeldekortIdParam, HistoriskMeldekortDetaljerDto> { param ->
                respond(
                    HistoriskMeldekortDetaljerDto(
                        arenaSkjemaFlate.historiskMeldekort(innloggetBruker(), param.meldekortId)
                    )
                )
            }

            route("{meldekortId}").post<MeldekortIdParam, Unit, MeldekortKorrigeringRequest> { meldekortId, request ->

            }
        }

        route("/test/proxy/meldegrupper").get<Unit, Any> {
            respond(arenaClient.meldegrupper(innloggetBruker()))
        }
        route("/test/proxy/meldekort").get<Unit, Any> {
            respond(arenaClient.person(innloggetBruker()) ?: "null")
        }
        route("/test/proxy/historiskemeldekort").get<Unit, Any> {
            respond(arenaClient.historiskeMeldekort(innloggetBruker(), antallMeldeperioder = 5))
        }
    }
}

private fun OpenAPIPipelineResponseContext<*>.innloggetBruker() =
    InnloggetBruker(
        ident = Ident(personBruker().pid),
        token = token().token(),
    )
