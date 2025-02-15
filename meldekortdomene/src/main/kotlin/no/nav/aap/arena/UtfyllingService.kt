package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import java.util.*

class UtfyllingService(
    private val utfyllingRepository: UtfyllingRepository,
    private val meldekortService: MeldekortService,
    skjemaService: SkjemaService,
) {
    private val flyt = UtfyllingFlyt(
        utfyllingRepository,
        listOf(
            BekreftSvarerÆrligSteg,
            JobbetIMeldeperiodenSteg,
            TimerArbeidetSteg,
            StemmerOpplysningeneSteg(skjemaService),
            KvitteringSteg,
        )
    )

    fun hentUtfylling(ident: Ident, meldekortId: MeldekortId): Utfylling? {
        return utfyllingRepository.last(ident, meldekortId, flyt)
    }

    fun hentEllerStartUtfylling(meldekortId: MeldekortId, innloggetBruker: InnloggetBruker): Utfylling {
        return hentUtfylling(innloggetBruker.ident, meldekortId) ?: opprettUtfylling(innloggetBruker, meldekortId)
    }

    private fun opprettUtfylling(
        innloggetBruker: InnloggetBruker,
        meldekortId: MeldekortId
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
            sendtInn = null,
            referanse = UUID.randomUUID()
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
        val utfall = utfylling.nesteSteg(innloggetBruker)
        utfyllingRepository.lagrUtfylling(utfall.getOrDefault(utfylling))
        return utfall.getOrThrow()
    }
}