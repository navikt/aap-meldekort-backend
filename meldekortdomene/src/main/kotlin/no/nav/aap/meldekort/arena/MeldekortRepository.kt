package no.nav.aap.meldekort.arena

interface MeldekortRepository {
    fun loadMeldekorttilstand(meldekortId: Long): Meldekorttilstand?
    fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand
}