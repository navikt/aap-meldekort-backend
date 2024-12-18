package no.nav.aap.meldekort.arena

import com.fasterxml.jackson.annotation.JsonTypeInfo
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
import no.nav.aap.meldekort.arenaflyt.MeldekortService
import no.nav.aap.meldekort.arenaflyt.Meldekortskjema
import no.nav.aap.meldekort.arenaflyt.Meldekorttilstand
import no.nav.aap.meldekort.arenaflyt.Meldeperiode
import no.nav.aap.meldekort.arenaflyt.Periode
import no.nav.aap.meldekort.arenaflyt.StegNavn
import no.nav.aap.meldekort.arenaflyt.TimerArbeidet
import java.time.LocalDate

fun NormalOpenAPIRoute.meldekortApi(
    meldekortService: MeldekortService,
    arenaService: ArenaService,
    arenaClient: ArenaClient,
) {
    route("/api/arena") {
        route("/meldeperiode").get<Unit, List<MeldeperiodeDto>> {
            val meldeperioder = arenaService.meldeperioder(innloggetBruker())
            respond(meldeperioder.map { MeldeperiodeDto(it) })
        }

        route("/meldekort/{meldekortId}") {
            class Params(
                @JsonValue @PathParam("meldekortId") val meldekortId: Long,
            )
            get<Params, MeldekortResponse> { params ->
                val nåværendeTilstand = meldekortService.hentEllerOpprettMeldekorttilstand(params.meldekortId, innloggetBruker())
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<Params, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                val innloggetBruker = innloggetBruker()

                val meldekorttilstand = meldekortService.konstruerMeldekorttilstand(
                    innloggetBruker = innloggetBruker,
                    meldekortId = params.meldekortId,
                    meldekortskjema = meldekortRequest.meldekort.tilDomene(),
                    stegNavn = meldekortRequest.nåværendeSteg,
                )

                val response = try {
                    MeldekortResponse(
                        meldekortService.lagreOgNeste(
                            meldekorttilstand = meldekorttilstand,
                            innloggetBruker = innloggetBruker,
                        )
                    )
                } catch (e: ArenaInnsendingFeiletException) {
                    MeldekortResponse(
                        meldekorttilstand = meldekorttilstand,
                        feil = InnsendingFeil(e.innsendingFeil)
                    )
                }

                respond(response)
            }

            route("/lagre").post<Params, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        meldekortService.lagre(meldekortService.konstruerMeldekorttilstand(
                            meldekortId = params.meldekortId,
                            meldekortskjema = meldekortRequest.meldekort.tilDomene(),
                            stegNavn = meldekortRequest.nåværendeSteg,
                            innloggetBruker = innloggetBruker(),
                        ))
                    )
                )
            }
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

private fun OpenAPIPipelineResponseContext<*>.innloggetBruker() =
    InnloggetBruker(
        ident = Ident(personBruker().pid),
        token = token().token(),
    )

@Suppress("unused")
class MeldeperiodeDto(
    val meldekortId: Long,
    val periode: PeriodeDto,
    val type: MeldeperiodeTypeDto,
    val klarForInnsending: Boolean,
    val kanEndres: Boolean,
) {
    constructor(meldeperiode: Meldeperiode) : this(
        meldekortId = meldeperiode.meldekortId,
        periode = PeriodeDto(meldeperiode.periode),
        type = MeldeperiodeTypeDto.fraDomene(meldeperiode.type),
        klarForInnsending = meldeperiode.kanSendes,
        kanEndres = meldeperiode.kanEndres,
    )

    enum class MeldeperiodeTypeDto {
        ORDINÆRT,
        ETTERREGISTRERT;

        companion object {
            fun fraDomene(type: Meldeperiode.Type): MeldeperiodeTypeDto {
                return when(type) {
                    Meldeperiode.Type.ORDINÆRT -> ORDINÆRT
                    Meldeperiode.Type.ETTERREGISTRERT -> ETTERREGISTRERT
                }
            }
        }

    }

}




@Suppress("unused")
class MeldekortSkjemaDto(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidetDto>,
    val stemmerOpplysningene: Boolean?
) {
    constructor(meldekortskjema: Meldekortskjema) : this(
        svarerDuSant = meldekortskjema.svarerDuSant,
        harDuJobbet = meldekortskjema.harDuJobbet,
        timerArbeidet = meldekortskjema.timerArbeidet.map { TimerArbeidetDto.fraDomene(it) },
        stemmerOpplysningene = meldekortskjema.stemmerOpplysningene,
    )

    fun tilDomene(): Meldekortskjema {
        return Meldekortskjema(
            svarerDuSant = svarerDuSant,
            harDuJobbet = harDuJobbet,
            timerArbeidet = timerArbeidet.map { it.tilDomene() },
            stemmerOpplysningene = stemmerOpplysningene
        )
    }
}

data class TimerArbeidetDto(
    val timer: Double?,
    val dato: LocalDate,
) {
    companion object {
        fun fraDomene(timerArbeidet: TimerArbeidet): TimerArbeidetDto {
            return TimerArbeidetDto(
                timer = timerArbeidet.timer,
                dato = timerArbeidet.dato
            )
        }
    }

    fun tilDomene(): TimerArbeidet {
        return TimerArbeidet(
            timer = timer,
            dato = dato
        )
    }
}

data class MeldekortRequest(
    val nåværendeSteg: StegNavn,
    val meldekort: MeldekortSkjemaDto
)

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode) : this(periode.fom, periode.tom)
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
)
sealed interface Feil

class InnsendingFeil(
    val innsendingFeil: List<ArenaInnsendingFeiletException.InnsendingFeil>
) : Feil

data class MeldekortResponse(
    val steg: StegNavn,
    val periode: PeriodeDto,
    val meldekort: MeldekortSkjemaDto,
    val feil: Feil?
) {
    constructor(meldekorttilstand: Meldekorttilstand, feil: Feil? = null) : this(
        steg = meldekorttilstand.steg.navn,
        meldekort = MeldekortSkjemaDto(meldekorttilstand.meldekortskjema),
        periode = PeriodeDto(meldekorttilstand.meldeperiode),
        feil = feil
    )
}
