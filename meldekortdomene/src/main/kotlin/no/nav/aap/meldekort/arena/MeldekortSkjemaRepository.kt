package no.nav.aap.meldekort.arena

interface MeldekortSkjemaRepository {
    fun loadMeldekorttilstand(meldekortId: Long, flyt: Flyt): Meldekorttilstand?
    fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand
}