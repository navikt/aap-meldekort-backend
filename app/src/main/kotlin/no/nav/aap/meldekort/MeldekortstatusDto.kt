package no.nav.aap.meldekort

import no.nav.aap.kelvin.MeldekortTilUtfylling
import java.time.LocalDate

data class MeldekortstatusDto(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<MeldekortTilUtfyllingDto>,
    val redirectUrl: String
)

data class MeldekortTilUtfyllingDto(
    val kanSendesFra: LocalDate,
    val kanFyllesUtFra: LocalDate?,
    val fristForInnsending: LocalDate
) {
    companion object {
        fun fraDomene(domene: MeldekortTilUtfylling): MeldekortTilUtfyllingDto {
            return MeldekortTilUtfyllingDto(
                kanSendesFra = domene.kanSendesFra,
                kanFyllesUtFra = domene.kanFyllesUtFra,
                fristForInnsending = domene.fristForInnsending
            )
        }
    }
}
