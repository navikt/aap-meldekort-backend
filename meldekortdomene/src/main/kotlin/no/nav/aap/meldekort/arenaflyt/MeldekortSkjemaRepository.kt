package no.nav.aap.meldekort.arenaflyt

interface MeldekortSkjemaRepository {
    fun loadMeldekorttilstand(meldekortId: Long, flyt: Flyt): Meldekorttilstand?
    fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand
}