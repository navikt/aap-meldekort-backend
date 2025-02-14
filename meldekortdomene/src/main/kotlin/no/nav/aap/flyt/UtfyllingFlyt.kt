package no.nav.aap.flyt

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.MeldekortId
import no.nav.aap.skjema.SkjemaTilstand.UTKAST
import no.nav.aap.skjema.Svar
import no.nav.aap.skjema.Skjema

interface Steg {
    val navn: StegNavn

    fun erRelevant(skjema: Skjema): Boolean {
        return true
    }

    fun oppfyllerFormkrav(skjema: Skjema): Boolean

    fun nesteEffekt(innloggetBruker: InnloggetBruker, skjema: Skjema) {
    }
}

class UtfyllingFlyt(
    val utfyllingRepository: UtfyllingRepository,
    val stegene: List<Steg>,
) {
    fun kjør(innloggetBruker: InnloggetBruker, utfylling: Utfylling): Result<Utfylling> {
        fun ferdig(skjema: Skjema, steg: StegNavn): Result<Utfylling> {
            val nyUtfylling = utfylling.medSteg(steg).copy(skjema = skjema)
            utfyllingRepository.lagrUtfylling(nyUtfylling)
            return Result.success(nyUtfylling)
        }

        val skjema = utfylling.skjema
        val utførteSteg = mutableSetOf<StegNavn>()

        for (steg in stegene) {
            if (!steg.erRelevant(skjema)) {
                continue
            }

            if (!steg.oppfyllerFormkrav(skjema)) {
                /* TODO: lag feil */
                return TODO()
            }

            try {
                steg.nesteEffekt(innloggetBruker, skjema)
            } catch (e: Exception) {
                TODO()
            }


            if (utfylling.steg.navn in utførteSteg) {
                return ferdig(skjema, steg.navn)
            }

            utførteSteg.add(steg.navn)
        }

        return ferdig(skjema, stegene.last().navn)
    }

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(stegene.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    override fun toString(): String {
        return stegene.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}

data class Utfylling(
    val ident: Ident,
    val meldekortId: MeldekortId,
    val flyt: UtfyllingFlyt,
    val steg: Steg,
    val skjema: Skjema,
) {
    init {
        check(ident == skjema.ident)
        check(meldekortId == skjema.meldekortId)
    }

    constructor(flyt: UtfyllingFlyt, steg: Steg, skjema: Skjema) : this(
        ident = skjema.ident,
        meldekortId = skjema.meldekortId,
        flyt = flyt,
        steg = steg,
        skjema = skjema,
    )

    fun nyPayload(payload: Svar): Utfylling {
        return copy(skjema = skjema.copy(svar = payload))
    }

    fun medSteg(steg: StegNavn): Utfylling {
        return copy(steg = flyt.stegForNavn(steg))
    }

    fun nesteSteg(innloggetBruker: InnloggetBruker): Result<Utfylling> {
        require(steg in flyt.stegene) { "steg $steg er ikke i flyt" }
        require(skjema.tilstand == UTKAST)
        return flyt.kjør(innloggetBruker, this)
    }

}

