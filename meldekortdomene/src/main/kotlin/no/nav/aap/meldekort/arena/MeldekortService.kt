package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker

class MeldekortService(
    private val meldekortSkjemaRepository: MeldekortSkjemaRepository,
    private val meldekortRepository: MeldekortRepository,
    private val arenaService: ArenaService
) {
    private val flyt = Flyt(
        BekreftSvarerÆrlig,
        JobbetIMeldeperioden,
        TimerArbeidet(this),
        Kvittering,
    )

    fun meldekorttilstand(meldekortId: Long, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        return meldekortSkjemaRepository.loadMeldekorttilstand(meldekortId, flyt)
            ?: meldekortSkjemaRepository.storeMeldekorttilstand(Meldekorttilstand(
                meldekortId = meldekortId,
                meldekortskjema = Meldekortskjema.tomtMeldekortskjema(),
                steg = BekreftSvarerÆrlig,
                meldeperiode = arenaService.meldeperioder(innloggetBruker).single { it.meldekortId == meldekortId }.periode
            ))
    }

    fun lagre(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
    }

    fun lagreOgNeste(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        try {
            return meldekortSkjemaRepository.storeMeldekorttilstand(
                meldekorttilstand.nesteTilstand(flyt, innloggetBruker)
            )
        } catch (e: Exception) {
            meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
            throw e
        }

    }

    fun meldekorttilstandMedSkjema(
        meldekortId: Long,
        meldekortskjema: Meldekortskjema,
        stegNavn: StegNavn
    ): Meldekorttilstand {
        val meldeperiode = meldekortSkjemaRepository.loadMeldekorttilstand(meldekortId, flyt)?.meldeperiode
            ?: error("Tilstand har ikke blitt opprettet for meldekort med id $meldekortId")

        val meldekorttilstand = Meldekorttilstand(
            meldekortId = meldekortId,
            meldekortskjema = meldekortskjema,
            steg = flyt.stegForNavn(stegNavn),
            meldeperiode = meldeperiode
        )
        return meldekorttilstand
    }

    fun sendInn(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker)  {
        val innsendtMeldekort = meldekorttilstand.innsendtMeldekort()

        meldekortRepository.storeMeldekort(innsendtMeldekort)
        val innsendingResponse = arenaService.sendInn(innsendtMeldekort, innloggetBruker)

        if (innsendingResponse.feil.isNotEmpty()) throw InnsendingFeiletException(innsendingResponse.feil)
    }
}