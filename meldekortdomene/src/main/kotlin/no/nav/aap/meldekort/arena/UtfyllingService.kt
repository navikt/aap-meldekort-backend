package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker

class UtfyllingService(
    private val utfyllingRepository: UtfyllingRepository,
    private val meldekortService: MeldekortService,
    skjemaService: SkjemaService,
) {
    private val flyt = UtfyllingFlyt(
        BekreftSvarerÆrligSteg,
        JobbetIMeldeperiodenSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg(skjemaService),
        KvitteringSteg,
    )

    fun hentUtfylling(ident: Ident, meldekortId: Long): Utfylling? {
        return utfyllingRepository.last(ident, meldekortId, flyt)
    }

    fun hentEllerStartUtfylling(meldekortId: Long, innloggetBruker: InnloggetBruker): Utfylling {
        return hentUtfylling(innloggetBruker.ident, meldekortId) ?: opprettUtfylling(innloggetBruker, meldekortId)
    }

    private fun opprettUtfylling(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long
    ): Utfylling {
        val meldeperiode = meldekortService.kommendeMeldekort(innloggetBruker)
            .orEmpty()
            .single { it.meldekortId == meldekortId }
            .periode
        val skjema = Skjema(
            meldekortId = meldekortId,
            payload = InnsendingPayload.tomtSkjema(meldeperiode),
            meldeperiode = meldeperiode,
            ident = innloggetBruker.ident,
            tilstand = SkjemaTilstand.UTKAST,
        )
        val utfylling = Utfylling(
            skjema = skjema,
            steg = flyt.stegForNavn(navn = BekreftSvarerÆrligSteg.navn),
            flyt = flyt,
        )
        utfyllingRepository.lagrUtfylling(utfylling)

        return utfylling
    }

    fun lagre(utfylling: Utfylling): Utfylling {
        utfyllingRepository.lagrUtfylling(utfylling)
        return utfylling
    }

    fun lagreOgNeste(
        innloggetBruker: InnloggetBruker,
        utfylling: Utfylling,
    ): Utfylling {
        return try {
            utfylling.nesteSteg(innloggetBruker).also {
                utfyllingRepository.lagrUtfylling(it)
            }
        } catch (e: Exception) {
            utfyllingRepository.lagrUtfylling(utfylling)
            throw e
        }
    }
}