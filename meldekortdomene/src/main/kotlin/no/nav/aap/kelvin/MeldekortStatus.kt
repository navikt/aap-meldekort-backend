package no.nav.aap.kelvin

import java.time.LocalDate

data class MeldekortTilUtfylling(
    val kanSendesFra: LocalDate,
    val fristForInnsending: LocalDate,
    val kanFyllesUtFra: LocalDate?
)
