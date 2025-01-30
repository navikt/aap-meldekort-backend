package no.nav.aap.meldekort.arena

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.personBruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.Periode
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    datasource: DataSource,
    arenaClient: ArenaClient,
) {
    route("/api/arena") {
        class MeldekortIdParam(
            @JsonValue @PathParam("meldekortId") val meldekortId: Long,
        )
        route("/skjema/{meldekortId}") {
            get<MeldekortIdParam, MeldekortResponse> { params ->
                val nåværendeTilstand = datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient)
                        .hentEllerOpprettUtfylling(innloggetBruker(), MeldekortId(params.meldekortId))

                }
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<MeldekortIdParam, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                respond(
                    try {
                        val response = datasource.transaction {
                            ArenaSkjemaFlate.konstruer(it, arenaClient).gåTilNesteSteg(
                                innloggetBruker = innloggetBruker(),
                                meldekortId = MeldekortId(params.meldekortId),
                                fraSteg = meldekortRequest.nåværendeSteg,
                                nyPayload = meldekortRequest.meldekort.tilDomene(),
                            )
                        }
                        MeldekortResponse(response)
                    } catch (e: ArenaInnsendingFeiletException) {
                        MeldekortResponse(
                            skjema = e.skjema!!,
                            feil = InnsendingFeil(e.innsendingFeil)
                        )
                    }
                )
            }

            route("/lagre").post<MeldekortIdParam, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                val response = datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient).lagreSteg(
                        ident = innloggetBruker().ident,
                        meldekortId = MeldekortId(params.meldekortId),
                        nyPayload = meldekortRequest.meldekort.tilDomene(),
                        settSteg = meldekortRequest.nåværendeSteg,
                    )
                }

                respond(
                    MeldekortResponse(response)
                )
            }

        }

        route("meldekort") {
            route("/neste").get<Unit, KommendeMeldekortDto> {
                val kommendeMeldekort = datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient).kommendeMeldekort(innloggetBruker())
                }
                respond(
                    KommendeMeldekortDto(
                        antallUbesvarteMeldekort = kommendeMeldekort.size,
                        nesteMeldekort = kommendeMeldekort.minByOrNull { it.periode }?.let {
                            NesteMeldekortDto(
                                meldeperiode = PeriodeDto(it.periode),
                                meldekortId = it.meldekortId.asLong,
                                tidligsteInnsendingsDato = it.tidligsteInnsendingsdato,
                                kanSendesInn = it.kanSendes
                            )
                        }
                    )
                )
            }

            route("/historisk").get<Unit, List<HistoriskMeldekortDto>> {
                val response = datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient).historiskeMeldekort(
                        innloggetBruker()
                    ).map { historiskMeldekort ->
                        HistoriskMeldekortDto(
                            meldeperiode = PeriodeDto(historiskMeldekort.periode),
                            status = historiskMeldekort.beregningStatus
                        )
                    }
                }

                respond(response)
            }

            route("/historisk/meldeperiode").post<Unit, List<HistoriskMeldekortDetaljerDto>, PeriodeDto> { _, meldeperiode ->
                val meldekortDetaljer = datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient).historiskeMeldekortDetaljer(
                        innloggetBruker(),
                        meldeperiode.let { periode -> Periode(periode.fom, periode.tom) }
                    )
                }

                respond(meldekortDetaljer.map(::HistoriskMeldekortDetaljerDto))
            }

            route("{meldekortId}").post<MeldekortIdParam, Unit, MeldekortKorrigeringRequest> { param, request ->
                datasource.transaction {
                    ArenaSkjemaFlate.konstruer(it, arenaClient).korrigerMeldekort(
                        innloggetBruker(),
                        MeldekortId(param.meldekortId),
                        request.timerArbeidet.map(TimerArbeidetDto::tilDomene)
                    )
                }
                respondWithStatus(HttpStatusCode.OK)
            }

        }

        if (configForKey("nais.cluster.name") in listOf("dev-gcp", "local")) {
            route("/test/proxy/meldegrupper").get<Unit, Any> {
                respond(arenaClient.meldegrupper(innloggetBruker()))
            }
            route("/test/proxy/meldekort").get<Unit, Any> {
                respond(arenaClient.person(innloggetBruker()) ?: "null")
            }
            route("/test/proxy/historiskemeldekort").get<Unit, Any> {
                respond(arenaClient.historiskeMeldekort(innloggetBruker(), antallMeldeperioder = 5))
            }
            route("/test/proxy/meldekortdetaljer/{meldekortId}").get<MeldekortIdParam, Any> { params ->
                respond(arenaClient.meldekortdetaljer(innloggetBruker(), MeldekortId(params.meldekortId)))
            }
            route("/test/proxy/korrigerte-meldekort/{meldekortId}").get<MeldekortIdParam, Any> { params ->
                respond(arenaClient.korrigertMeldekort(innloggetBruker(), MeldekortId(params.meldekortId)))
            }
        }
    }
}

private fun OpenAPIPipelineResponseContext<*>.innloggetBruker() =
    InnloggetBruker(
        ident = Ident(personBruker().pid),
        token = token().token(),
    )
