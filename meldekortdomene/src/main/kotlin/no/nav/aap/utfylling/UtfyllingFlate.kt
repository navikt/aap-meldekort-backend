package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import java.time.LocalDateTime

interface UtfyllingFlate {
    class Metadata(
        val referanse: UtfyllingReferanse,
        val periode: Periode,
        val antallUbesvarteMeldeperioder: Int,
        val tidligsteInnsendingstidspunkt: LocalDateTime?,
        val fristForInnsending: LocalDateTime?,
        val kanSendesInn: Boolean,
        val brukerHarVedtakIKelvin:  Boolean? = null,
        val brukerHarSakUnderBehandling: Boolean? = null,
    )

    class StartUtfyllingResponse(
        val metadata: Metadata?,
        val utfylling: Utfylling?,
        val feil: String?,
    )

    class UtfyllingResponse(
        val metadata: Metadata,
        val utfylling: Utfylling,
        val feil: String?,
    )

    fun startUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): StartUtfyllingResponse

    fun startKorrigering(innloggetBruker: InnloggetBruker, periode: Periode): StartUtfyllingResponse

    fun hentUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse): UtfyllingResponse?

    fun lagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse

    fun nesteOgLagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse

    fun slettUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse)
}
