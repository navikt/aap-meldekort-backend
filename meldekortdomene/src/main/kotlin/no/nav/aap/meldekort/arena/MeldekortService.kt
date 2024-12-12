package no.nav.aap.meldekort.arena

class MeldekortService(
    private val meldekortSkjemaRepository: MeldekortSkjemaRepository,
    private val meldekortRepository: MeldekortRepository
) {
    private val flyt = Flyt(
        BekreftSvarerÆrlig,
        JobbetIMeldeperioden,
        TimerArbeidet(this),
        Kvittering,
    )

    fun meldekorttilstand(meldekortId: Long): Meldekorttilstand {
        return meldekortSkjemaRepository.loadMeldekorttilstand(meldekortId, flyt) ?: Meldekorttilstand(
            meldekortId = meldekortId,
            meldekortskjema = Meldekortskjema.tomtMeldekort(),
            steg = BekreftSvarerÆrlig,
        )
    }

    fun lagre(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
    }

    fun lagreOgNeste(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return try {
            val nesteTilstand = Meldekorttilstand(
                meldekortId = meldekorttilstand.meldekortId,
                steg = flyt.nesteSteg(meldekorttilstand),
                meldekortskjema = meldekorttilstand.meldekortskjema
            )

            meldekortSkjemaRepository.storeMeldekorttilstand(nesteTilstand)
        } catch (e: Exception) {
            meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
            throw e
        }

    }

    fun stegForNavn(stegNavn: StegNavn): Steg {
        return flyt.stegForNavn(stegNavn)
    }

    fun sendInn(meldekortskjema: Meldekortskjema, meldekortId: Long) {
        //TODO - send inn i arena
        meldekortRepository.storeMeldekort(
            meldekortskjema.innsendtMeldekort(meldekortId)
        )
    }
}