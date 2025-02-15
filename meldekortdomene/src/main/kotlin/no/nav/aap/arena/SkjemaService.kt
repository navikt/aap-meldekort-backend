package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.journalføring.JournalføringService
import java.time.LocalDateTime
import java.util.*

class SkjemaService(
    private val meldekortService: MeldekortService,
    private val skjemaRepository: SkjemaRepository,
    private val journalføringService: JournalføringService
) {
    fun finnSkjema(ident: Ident, meldekortId: MeldekortId): Skjema? {
        return skjemaRepository.last(ident, meldekortId)
    }

    fun timerArbeidet(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): List<TimerArbeidet>? {
        return skjemaRepository.last(innloggetBruker.ident, meldekortId)?.payload?.timerArbeidet
    }

    fun sendInnKorrigering(
        innloggetBruker: InnloggetBruker,
        originalMeldekortId: MeldekortId,
        timerArbeidet: List<TimerArbeidet>
    ) {
        val nyttMeldekort = meldekortService.nyttMeldekortForKorrigering(innloggetBruker, originalMeldekortId)

        val skjema = Skjema(
            tilstand = SkjemaTilstand.UTKAST,
            meldekortId = nyttMeldekort.meldekortId,
            ident = innloggetBruker.ident,
            meldeperiode = nyttMeldekort.periode,
            sendtInn = LocalDateTime.now(),
            referanse = UUID.randomUUID(),
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
        meldekortService.sendInn(skjema, innloggetBruker)
        skjemaRepository.lagrSkjema(
            skjema.copy(
                tilstand = SkjemaTilstand.SENDT_ARENA,
                sendtInn = LocalDateTime.now(),
            )
        )

        journalføringService.journalfør(innloggetBruker.ident, skjema.meldekortId)
    }
}