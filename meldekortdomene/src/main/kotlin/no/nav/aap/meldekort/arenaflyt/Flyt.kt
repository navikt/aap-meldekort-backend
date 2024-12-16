package no.nav.aap.meldekort.arenaflyt

import no.nav.aap.meldekort.InnloggetBruker

class Flyt private constructor(
    private val steg: List<Steg>
) {
    constructor(vararg steg: Steg) : this(listOf(*steg))

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(steg.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    fun nesteSteg(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker): Steg {
        require(meldekorttilstand.steg in steg) { "steg ${meldekorttilstand.steg} er ikke i flyt" }
        return stegForNavn(
            meldekorttilstand.steg.nesteSteg(meldekorttilstand, innloggetBruker)
        )
    }

    override fun toString(): String {
        return steg.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}