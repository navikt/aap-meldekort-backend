package no.nav.aap.meldekort.arena

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
class MeldekortSkjemaRepositoryFake: MeldekortSkjemaRepository {
    private val meldekorttilstander: MutableList<MeldekorttilstandEntity> = mutableListOf()

    override fun loadMeldekorttilstand(meldekortId: Long, flyt: Flyt): Meldekorttilstand? {
        return meldekorttilstander.singleOrNull { it.meldekortId == meldekortId && it.aktiv }?.tilDomene(flyt)
    }

    override fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        setToInactive()
        this.meldekorttilstander.add(
            MeldekorttilstandEntity.fraDomene(meldekorttilstand = meldekorttilstand, aktiv = true)
        )
        return meldekorttilstand
    }

    private fun setToInactive() {
        val aktivTilstand = meldekorttilstander.single { it.aktiv }

        meldekorttilstander.apply {
            set(this.indexOf(aktivTilstand), aktivTilstand.copy(aktiv = false))
        }
    }
}