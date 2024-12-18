package no.nav.aap.meldekort.arenaflyt

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.ArenaService

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

    fun hentEllerOpprettMeldekorttilstand(meldekortId: Long, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        return meldekortSkjemaRepository.loadMeldekorttilstand(innloggetBruker.ident, meldekortId, flyt)
            ?: opprettMeldekorttilstand(innloggetBruker, meldekortId)
    }

    private fun opprettMeldekorttilstand(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long
    ): Meldekorttilstand {
        val meldeperiode = arenaService.meldeperioder(innloggetBruker).single { it.meldekortId == meldekortId }.periode
        return meldekortSkjemaRepository.storeMeldekorttilstand(
            konstruerMeldekorttilstand(
                innloggetBruker = innloggetBruker,
                meldekortId = meldekortId,
                meldekortskjema = Meldekortskjema.tomtMeldekortskjema(meldeperiode),
                stegNavn = BekreftSvarerÆrligSteg.navn,
                meldeperiode = meldeperiode,
            )
        )
    }

    fun lagre(meldekorttilstand: Meldekorttilstand): Meldekorttilstand {
        return meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
    }

    fun lagreOgNeste(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        return try {
            meldekortSkjemaRepository.storeMeldekorttilstand(
                meldekorttilstand.nesteTilstand(flyt, innloggetBruker)
            )
        } catch (e: Exception) {
            meldekortSkjemaRepository.storeMeldekorttilstand(meldekorttilstand)
            throw e
        }
    }

    fun konstruerMeldekorttilstand(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long,
        meldekortskjema: Meldekortskjema,
        stegNavn: StegNavn,
        meldeperiode: Periode? = null,
    ): Meldekorttilstand {
        return Meldekorttilstand(
            meldekortId = meldekortId,
            meldekortskjema = meldekortskjema,
            steg = flyt.stegForNavn(stegNavn),
            meldeperiode = meldeperiode ?: finnMeldeperiode(innloggetBruker, meldekortId),
            ident = innloggetBruker.ident,
        )
    }

    private fun finnMeldeperiode(innloggetBruker: InnloggetBruker, meldekortId: Long): Periode {
        return meldekortSkjemaRepository.loadMeldekorttilstand(innloggetBruker.ident, meldekortId, flyt)
            ?.meldeperiode
            ?: arenaService.meldeperioder(innloggetBruker).single { it.meldekortId == meldekortId }.periode
    }

    /* TODO:
     *  - håndtere at systemet krasjer mellom innsending til kontroll og lagring til database
     */

    fun sendInn(meldekorttilstand: Meldekorttilstand, innloggetBruker: InnloggetBruker) {
        val innsendtMeldekort = meldekorttilstand.innsendtMeldekort()
        arenaService.sendInn(innsendtMeldekort, innloggetBruker)
        meldekortRepository.storeMeldekort(innsendtMeldekort)
    }
}