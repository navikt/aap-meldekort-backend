package no.nav.aap.meldekort.arena

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
class MeldekortSkjemaRepositoryFake: MeldekortSkjemaRepository {
    private val meldekorttilstand: HashMap<Long, Meldekorttilstand> = HashMap()

    override fun loadMeldekorttilstand(meldekortId: Long, flyt: Flyt): Meldekorttilstand? {
        return meldekorttilstand[meldekortId]
    }

    override fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        this.meldekorttilstand[meldekorttilstand.meldekortId] = meldekorttilstand
        return meldekorttilstand
    }
}