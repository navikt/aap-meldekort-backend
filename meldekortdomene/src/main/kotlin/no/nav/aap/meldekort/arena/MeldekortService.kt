package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker

class MeldekortService(
    private val meldekortSkjemaRepository: MeldekortSkjemaRepository,
    private val meldekortRepository: MeldekortRepository,
    private val arenaService: ArenaService
) {
    private val flyt = Flyt(
        BekreftSvarerÆrligSteg,
        JobbetIMeldeperiodenSteg,
        TimerArbeidetSteg(this),
        KvitteringSteg,
    )

    fun meldekorttilstand(meldekortId: Long, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        val eksisterendeMeldekorttilstand = meldekortSkjemaRepository.loadMeldekorttilstand(meldekortId, flyt)

        if (eksisterendeMeldekorttilstand != null) return eksisterendeMeldekorttilstand

        val meldeperiode = arenaService.meldeperioder(innloggetBruker).single { it.meldekortId == meldekortId }.periode

        return meldekortSkjemaRepository.storeMeldekorttilstand(Meldekorttilstand(
                meldekortId = meldekortId,
                meldekortskjema = Meldekortskjema.tomtMeldekortskjema(meldeperiode),
                steg = BekreftSvarerÆrligSteg,
                meldeperiode = meldeperiode
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

    /* TODO:
     *  - håndtere at systemet krasjer mellom innsending til kontroll og lagring til database
     */

    fun sendInn(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker)  {
        val innsendtMeldekort = meldekorttilstand.innsendtMeldekort()

        val innsendingResponse = arenaService.sendInn(innsendtMeldekort, innloggetBruker)
        if (innsendingResponse.feil.isNotEmpty()) throw InnsendingFeiletException(innsendingResponse.feil)
        meldekortRepository.storeMeldekort(innsendtMeldekort)
    }
}