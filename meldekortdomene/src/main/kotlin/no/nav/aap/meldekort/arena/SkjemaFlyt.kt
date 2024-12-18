package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker

class SkjemaFlyt private constructor(
    private val steg: List<Steg>
) {
    constructor(vararg steg: Steg) : this(listOf(*steg))

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(steg.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): Steg {
        require(skjema.steg in steg) { "steg ${skjema.steg} er ikke i flyt" }
        return stegForNavn(
            skjema.steg.nesteSteg(skjema, innloggetBruker)
        )
    }

    override fun toString(): String {
        return steg.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}