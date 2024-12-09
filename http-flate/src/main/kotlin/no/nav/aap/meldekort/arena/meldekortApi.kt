package no.nav.aap.meldekort.arena

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

        route("/meldekort/meldekortId}") {
            get<Long, MeldekortResponse> { meldekortId ->
                val nåværendeTilstand = meldekortService.meldekorttilstand(meldekortId)
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<Long, MeldekortResponse, MeldekortRequest> { meldekortId, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        meldekortService.lagreOgNeste(meldekortRequest.meldekorttilstand(meldekortService, meldekortId))
                    )
                )
            }

            route("/lagre").post<Long, MeldekortResponse, MeldekortRequest> { meldekortId, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        meldekortService.lagre(meldekortRequest.meldekorttilstand(meldekortService, meldekortId))
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
    val timerArbeidet: List<Int?>,
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
) {
    fun meldekorttilstand(meldekortService: MeldekortService, meldekortId: Long): Meldekorttilstand {
        return Meldekorttilstand(
            meldekortId = meldekortId,
            steg = meldekortService.stegForNavn(nåværendeSteg),
            meldekortskjema = meldekort.tilDomene(),
        )
    }
}

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode): this(periode.fom, periode.tom)
}

data class MeldekortResponse(
    val steg: StegNavn,
    val periode: PeriodeDto,
    val meldekort: MeldekortDto,
) {
    constructor(meldekorttilstand: Meldekorttilstand) : this(
        steg = meldekorttilstand.steg.navn,
        meldekort = MeldekortDto(meldekorttilstand.meldekortskjema),
        periode = PeriodeDto(LocalDate.now().minusDays(14), LocalDate.now()),
    )
}
