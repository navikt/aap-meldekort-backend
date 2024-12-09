package no.nav.aap.meldekort.arena

class MeldekortService(
    private val meldekortRepository: MeldekortRepository,
) {
    private val flyt = listOf(
        BekreftSvarerÆrlig,
        JobbetIMeldeperioden,
        TimerArbeidet(this),
        Kvittering,
    )

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
        val utfall = meldekorttilstand.steg.nesteSteg(meldekorttilstand.meldekortskjema)
        val nesteSteg = when (utfall) {
            is InnsendingFeilet -> meldekorttilstand.steg.navn
            is GåTilSteg -> utfall.steg
        }
        val nesteTilstand = Meldekorttilstand(
            meldekortId = meldekorttilstand.meldekortId,
            steg = stegForNavn(nesteSteg),
            meldekortskjema = meldekorttilstand.meldekortskjema
        )
        return meldekortRepository.storeMeldekorttilstand(nesteTilstand)
    }

    fun stegForNavn(stegNavn: StegNavn): Steg {
        return requireNotNull(flyt.find { it.navn == stegNavn }) {
            "steg $stegNavn finnes ikke i flyt (${flyt.joinToString { it.navn.toString() }})"
        }
    }

    fun sendInn(meldekortskjema: Meldekortskjema) {


    }
}