package no.nav.aap.meldekort

import no.nav.aap.kelvin.Meldekortstatus
import no.nav.aap.kelvin.MeldekortTilUtfylling
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.time.LocalDate

data class MeldekortstatusDto(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<MeldekortTilUtfyllingDto>,
    val redirectUrl: String
) {
    companion object {
        fun fraDomene(domene: Meldekortstatus): MeldekortstatusDto {
            return MeldekortstatusDto(
                harInnsendteMeldekort = domene.harInnsendteMeldekort,
                meldekortTilUtfylling = domene.meldekortTilUtfylling.map { meldekort -> MeldekortTilUtfyllingDto.fraDomene(meldekort) },
                redirectUrl = requiredConfigForKey("aap.meldekort.lenke"),
            )
        }
    }
}

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
