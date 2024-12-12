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
import no.nav.aap.meldekort.InnloggetBruker
import java.time.LocalDate

fun NormalOpenAPIRoute.meldekortApi(
    meldekortService: MeldekortService,
    arenaService: ArenaService,
    arena: Arena,
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
                val nåværendeTilstand = meldekortService.meldekorttilstand(params.meldekortId, innloggetBruker())
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<Params, MeldekortResponse, MeldekortRequest> { params, meldekortRequest ->
                val meldekorttilstand = meldekortService.meldekorttilstandMedSkjema(
                    meldekortId = params.meldekortId,
                    meldekortskjema = meldekortRequest.meldekort.tilDomene(),
                    stegNavn = meldekortRequest.nåværendeSteg
                )

                val response = try {
                    MeldekortResponse(
                        meldekortService.lagreOgNeste(
                            meldekorttilstand = meldekorttilstand,
                            innloggetBruker = innloggetBruker()
                        )
                    )
                } catch (e: InnsendingFeiletException) {
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
                        meldekortService.lagre(meldekortService.meldekorttilstandMedSkjema(
                            meldekortId = params.meldekortId,
                            meldekortskjema = meldekortRequest.meldekort.tilDomene(),
                            stegNavn = meldekortRequest.nåværendeSteg
                        ))
                    )
                )
            }
        }
    }

    route("/test/proxy/meldegrupper").get<Unit, Any> { call ->
        respond(arena.meldegrupper(innloggetBruker()))
    }
    route("/test/proxy/meldekort").get<Unit, Any> { call ->
        respond(arena.person(innloggetBruker()) ?: "null")
    }
}

private fun OpenAPIPipelineResponseContext<*>.innloggetBruker() =
    InnloggetBruker(
        ident = personBruker().pid,
        token = token().token(),
    )

@Suppress("unused")
class MeldeperiodeDto(
    val meldekortId: Long,
    val periode: PeriodeDto,
    val status: Status,
) {
    enum class Status {
        KLAR_FOR_INNSENDING,
    }

    constructor(meldeperiode: Meldeperiode) : this(
        meldekortId = meldeperiode.meldekortId,
        periode = PeriodeDto(meldeperiode.periode),
        status = Status.KLAR_FOR_INNSENDING
    )
}


@Suppress("unused")
class MeldekortDto(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Double?>,
    val stemmerOpplysningene: Boolean?
) {
    constructor(meldekortskjema: Meldekortskjema) : this(
        svarerDuSant = meldekortskjema.svarerDuSant,
        harDuJobbet = meldekortskjema.harDuJobbet,
        timerArbeidet = meldekortskjema.timerArbeidet,
        stemmerOpplysningene = meldekortskjema.stemmerOpplysningene,
    )

    fun tilDomene(): Meldekortskjema {
        return Meldekortskjema(
            svarerDuSant = svarerDuSant,
            harDuJobbet = harDuJobbet,
            timerArbeidet = timerArbeidet,
            stemmerOpplysningene = stemmerOpplysningene
        )
    }
}

data class MeldekortRequest(
    val nåværendeSteg: StegNavn,
    val meldekort: MeldekortDto
)

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode) : this(periode.fom, periode.tom)
}

interface Feil
class InnsendingFeil(
    val innsendingFeil: List<ArenaService.InnsendingFeil>
) : Feil

data class MeldekortResponse(
    val steg: StegNavn,
    val periode: PeriodeDto,
    val meldekort: MeldekortDto,
    val feil: Feil?
) {
    constructor(meldekorttilstand: Meldekorttilstand, feil: Feil? = null) : this(
        steg = meldekorttilstand.steg.navn,
        meldekort = MeldekortDto(meldekorttilstand.meldekortskjema),
        periode = PeriodeDto(LocalDate.now().minusDays(14), LocalDate.now()),
        feil = feil
    )
}
