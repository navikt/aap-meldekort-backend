package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker

class SkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val meldekortService: MeldekortService
) {
    private val flyt = SkjemaFlyt(
        BekreftSvarerÆrligSteg,
        JobbetIMeldeperiodenSteg,
        TimerArbeidetSteg(this),
        KvitteringSteg,
    )

    fun hentSkjema(ident: Ident, meldekortId: Long): Skjema? {
        return skjemaRepository.last(ident, meldekortId, flyt)
    }

    fun hentEllerOpprettSkjema(meldekortId: Long, innloggetBruker: InnloggetBruker): Skjema {
        return hentSkjema(innloggetBruker.ident, meldekortId) ?: opprettSkjema(innloggetBruker, meldekortId)
    }

    private fun opprettSkjema(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long
    ): Skjema {
        val meldeperiode = meldekortService.kommendeMeldekort(innloggetBruker)
            .orEmpty()
            .single { it.meldekortId == meldekortId }
            .periode
        val skjema = Skjema(
            meldekortId = meldekortId,
            payload = InnsendingPayload.tomtSkjema(meldeperiode),
            steg = flyt.stegForNavn(navn = BekreftSvarerÆrligSteg.navn),
            meldeperiode = meldeperiode,
            ident = innloggetBruker.ident,
            flyt = flyt,
            tilstand = SkjemaTilstand.UTKAST,
        )
        skjemaRepository.lagrSkjema(skjema)

        return skjema
    }

    fun lagre(skjema: Skjema): Skjema {
        skjemaRepository.lagrSkjema(skjema)
        return skjema
    }

    fun lagreOgNeste(
        innloggetBruker: InnloggetBruker,
        skjema: Skjema,
    ): Skjema {
        return try {
            skjema.nesteSteg(innloggetBruker).also {
                skjemaRepository.lagrSkjema(it)
            }
        } catch (e: Exception) {
            skjemaRepository.lagrSkjema(skjema)
            throw e
        }
    }

    fun sendInn(skjema: Skjema, innloggetBruker: InnloggetBruker) {
        skjemaRepository.lagrSkjema(skjema.copy(tilstand = SkjemaTilstand.FORSØKER_Å_SENDE_TIL_ARENA))
        meldekortService.sendInn(skjema, innloggetBruker)
        skjemaRepository.lagrSkjema(skjema.copy(tilstand = SkjemaTilstand.SENDT_ARENA))
    }
}