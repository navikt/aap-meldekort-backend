package no.nav.aap.meldekort.arena

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
class MeldekortSkjemaRepositoryFake: MeldekortSkjemaRepository {
    private var meldekorttilstand: Meldekorttilstand? = null

    override fun loadMeldekorttilstand(meldekortId: Long, flyt: Flyt): Meldekorttilstand? {
        return meldekorttilstand
    }

    override fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        this.meldekorttilstand = meldekorttilstand
        return meldekorttilstand
    }
}