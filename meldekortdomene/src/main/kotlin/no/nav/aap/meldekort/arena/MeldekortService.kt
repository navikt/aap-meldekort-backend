package no.nav.aap.meldekort.arena

class MeldekortService(
    private val meldekortRepository: MeldekortRepository,
) {
    fun meldekorttilstand(meldekortId: Long): Meldekorttilstand {
        return meldekortRepository.loadMeldekorttilstand(meldekortId) ?: Meldekorttilstand(
            meldekortId = meldekortId,
            meldekortskjema = Meldekortskjema.tomtMeldekort(),
            steg = BekreftSvarerÆrlig,
        )
    }

    fun lagre(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return meldekortRepository.storeMeldekorttilstand(meldekorttilstand)
    }

    fun lagreOgNeste(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        val nesteSteg = meldekorttilstand.steg.nesteSteg(meldekorttilstand.meldekortskjema)
        if (nesteSteg == null) {
            error("")
        }
        val nesteTilstand = Meldekorttilstand(
            meldekortId = meldekorttilstand.meldekortId,
            steg = nesteSteg,
            meldekortskjema = meldekorttilstand.meldekortskjema
        )
        return meldekortRepository.storeMeldekorttilstand(nesteTilstand)
    }
}