package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.SkjemaTilstand.UTKAST

class UtfyllingFlyt private constructor(
    val steg: List<Steg>
) {
    constructor(vararg steg: Steg) : this(listOf(*steg))

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(steg.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    override fun toString(): String {
        return steg.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}

data class Utfylling(
    val ident: Ident,
    val meldekortId: Long,
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

    fun nyPayload(payload: InnsendingPayload): Utfylling {
        return copy(skjema = skjema.copy(payload = payload))
    }

    fun medSteg(steg: StegNavn): Utfylling {
        return copy(steg = flyt.stegForNavn(steg))
    }

    fun nesteSteg(innloggetBruker: InnloggetBruker): Utfylling {
        require(steg in flyt.steg) { "steg $steg er ikke i flyt" }
        require(skjema.tilstand == UTKAST)
        return copy(
            steg = flyt.stegForNavn(steg.nesteSteg(this.skjema, innloggetBruker))
        )
    }

    fun validerUtkast() {
        check(skjema.tilstand == UTKAST)
        // TODO: kast exception hvis validering feil?
    }
}

