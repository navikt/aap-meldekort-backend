package no.nav.aap.utfylling

import no.nav.aap.Periode
import java.time.LocalDateTime
import no.nav.aap.Ident

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
        val visFrist: Boolean = true,
        val flytNavn: UtfyllingFlytNavn?,
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

    fun startUtfylling(ident: Ident, periode: Periode): StartUtfyllingResponse

    fun startKorrigering(ident: Ident, periode: Periode): StartUtfyllingResponse

    fun hentUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): UtfyllingResponse?

    fun lagre(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse

    fun nesteOgLagre(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse

    fun slettUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse)
}
