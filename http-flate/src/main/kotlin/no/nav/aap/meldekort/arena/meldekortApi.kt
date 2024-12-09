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
        get<Unit, MeldekortResponse> { _ ->
            val nåværendeTilstand = meldekortService.meldekorttilstand()
            respond(MeldekortResponse(nåværendeTilstand))
        }

        route("/neste-steg").post<Unit, MeldekortResponse, MeldekortRequest> { _, meldekortRequest ->
            respond(MeldekortResponse(meldekortService.lagreOgNeste(meldekortRequest.meldekorttilstand())))
        }

        route("/lagre").post<Unit, MeldekortResponse, MeldekortRequest> { _, meldekortRequest ->
            respond(MeldekortResponse(meldekortService.lagre(meldekortRequest.meldekorttilstand())))
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
}

data class MeldekortRequest(
    val nåværendeSteg: StegNavn,
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
) {
    fun meldekorttilstand(): Meldekorttilstand {
        return Meldekorttilstand(
            steg = nåværendeSteg.steg,
            meldekort = Meldekort(
                svarerDuSant = svarerDuSant,
                harDuJobbet = harDuJobbet,
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = stemmerOpplysningene
            )
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
