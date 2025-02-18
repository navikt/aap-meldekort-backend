package no.nav.aap.meldekort

import no.nav.aap.Periode
import java.time.LocalDate

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode) : this(periode.fom, periode.tom)
}
