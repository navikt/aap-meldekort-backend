package no.nav.aap.meldekort.arena

interface MeldekortRepository {
    fun loadMeldekorttilstand(): Meldekorttilstand?
    fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand
}