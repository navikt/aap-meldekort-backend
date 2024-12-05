package no.nav.aap.meldekort.arena

class MeldekortService(
    private val meldekortRepository: MeldekortRepository,
) {
    fun meldekorttilstand(): Meldekorttilstand {
        return meldekortRepository.loadMeldekorttilstand() ?: Meldekorttilstand(
            meldekort = Meldekort.tomtMeldekort(),
            steg = BekreftSvarerÆrlig,
        )
    }

    fun lagre(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return meldekortRepository.storeMeldekorttilstand(meldekorttilstand)
    }

    fun lagreOgNeste(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        val nesteSteg = meldekorttilstand.steg.nesteSteg(meldekorttilstand.meldekort)
        if (nesteSteg == null) {
            error("")
        }
        val nesteTilstand = Meldekorttilstand(
            steg = nesteSteg,
            meldekort = meldekorttilstand.meldekort
        )
        return meldekortRepository.storeMeldekorttilstand(nesteTilstand)
    }
}