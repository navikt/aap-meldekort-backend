package no.nav.aap.meldekort.arena

class Meldekorttilstand(
    val meldekortId: Long,
    val meldekortskjema: Meldekortskjema,
    val steg: Steg,
) {
    fun nesteSteg(): NesteUtfall {
        return steg.nesteSteg(this)
    }
}