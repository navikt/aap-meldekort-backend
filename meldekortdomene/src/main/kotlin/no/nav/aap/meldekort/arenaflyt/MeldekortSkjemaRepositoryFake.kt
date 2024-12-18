package no.nav.aap.meldekort.arenaflyt

import no.nav.aap.meldekort.Ident

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
class MeldekortSkjemaRepositoryFake: MeldekortSkjemaRepository {
    private val meldekorttilstand: HashMap<Pair<Ident, Long>, Meldekorttilstand> = HashMap()

    override fun loadMeldekorttilstand(ident: Ident, meldekortId: Long, flyt: Flyt): Meldekorttilstand? {
        return meldekorttilstand[ident to meldekortId]
    }

    override fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        this.meldekorttilstand[meldekorttilstand.ident to meldekorttilstand.meldekortId] = meldekorttilstand
        return meldekorttilstand
    }
}