package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker

class SkjemaService(
    private val meldekortService: MeldekortService,
    private val skjemaRepository: SkjemaRepository,
) {
    fun timerArbeidet(innloggetBruker: InnloggetBruker, meldekortId: Long): List<TimerArbeidet>? {
        return skjemaRepository.last(innloggetBruker.ident, meldekortId)?.payload?.timerArbeidet
    }

    fun sendInnKorrigering(
        innloggetBruker: InnloggetBruker,
        originalMeldekortId: Long,
        timerArbeidet: List<TimerArbeidet>
    ) {
        val nyttMeldekort = meldekortService.nyttMeldekortForKorrigering(innloggetBruker, originalMeldekortId)

        val skjema = Skjema(
            tilstand = SkjemaTilstand.UTKAST,
            meldekortId = nyttMeldekort.meldekortId,
            ident = innloggetBruker.ident,
            meldeperiode = nyttMeldekort.periode,
            payload = InnsendingPayload(
                svarerDuSant = true,
                harDuJobbet = true,
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = true
            )
        )

        sendInn(skjema, innloggetBruker)
    }


    fun sendInn(skjema: Skjema, innloggetBruker: InnloggetBruker) {
        skjemaRepository.lagrSkjema(
            skjema.copy(tilstand = SkjemaTilstand.FORSØKER_Å_SENDE_TIL_ARENA)
        )
        meldekortService.sendInn(skjema, innloggetBruker)
        skjemaRepository.lagrSkjema(skjema.copy(tilstand = SkjemaTilstand.SENDT_ARENA))
    }
}