package no.nav.aap.meldekort.arena

class MeldekortService(
    private val meldekortRepository: MeldekortRepository,
) {
    fun meldekorttilstand(meldekortId: Long): Meldekorttilstand {
        return meldekortRepository.loadMeldekorttilstand(meldekortId) ?: Meldekorttilstand(
            meldekortId = meldekortId,
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
            meldekortId = meldekorttilstand.meldekortId,
            steg = nesteSteg,
            meldekort = meldekorttilstand.meldekort
        )
        return meldekortRepository.storeMeldekorttilstand(nesteTilstand)
    }
}