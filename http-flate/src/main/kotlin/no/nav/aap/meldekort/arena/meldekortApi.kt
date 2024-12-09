package no.nav.aap.meldekort.arena

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.httpklient.auth.personBruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.meldekort.InnloggetBruker

fun NormalOpenAPIRoute.meldekortApi(
    meldekortService: MeldekortService,
    arena: Arena,
) {
    route("/api/arena/meldekort") {
        route("/{meldekortId}") {
            get<Long, MeldekortResponse> { meldekortId ->
                val nåværendeTilstand = meldekortService.meldekorttilstand(meldekortId)
                respond(MeldekortResponse(nåværendeTilstand))
            }

            route("/neste-steg").post<Long, MeldekortResponse, MeldekortRequest> { meldekortId, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        meldekortService.lagreOgNeste(meldekortRequest.meldekorttilstand(meldekortId))
                    )
                )
            }

            route("/lagre").post<Long, MeldekortResponse, MeldekortRequest> { meldekortId, meldekortRequest ->
                respond(
                    MeldekortResponse(
                        meldekortService.lagre(meldekortRequest.meldekorttilstand(meldekortId))
                    )
                )
            }

        }
    }
    route("/test/meldegrupper").get<Unit, Any> { call ->
        val innloggetBruker = InnloggetBruker(
            ident = personBruker().pid,
            token = token().token(),
        )
        respond(arena.meldegrupper(innloggetBruker))
    }
    route("/test/meldekort").get<Unit, Any> { call ->
        val innloggetBruker = InnloggetBruker(
            ident = personBruker().pid,
            token = token().token(),
        )
        respond(arena.meldekort(innloggetBruker) ?: "null")
    }
}

@Suppress("unused")
class MeldekortDto(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
) {
    constructor(meldekort: Meldekort) : this(
        svarerDuSant = meldekort.svarerDuSant,
        harDuJobbet = meldekort.harDuJobbet,
        timerArbeidet = meldekort.timerArbeidet,
        stemmerOpplysningene = meldekort.stemmerOpplysningene,
    )

    fun tilDomene(): Meldekort {
        return Meldekort(
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
    fun meldekorttilstand(meldekortId: Long): Meldekorttilstand {
        return Meldekorttilstand(
            meldekortId = meldekortId,
            steg = nåværendeSteg.steg,
            meldekort = meldekort.tilDomene(),
        )
    }
}

data class MeldekortResponse(
    val steg: StegNavn,
    val meldekort: MeldekortDto,
) {
    constructor(meldekorttilstand: Meldekorttilstand) : this(
        steg = meldekorttilstand.steg.navn,
        meldekort = MeldekortDto(meldekorttilstand.meldekort),
    )
}
