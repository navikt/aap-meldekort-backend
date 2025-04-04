package no.nav.aap.meldekort

import no.nav.aap.Periode
import java.time.LocalDate

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode) : this(periode.fom, periode.tom)

    companion object {
        fun ellerNull(periode: Periode?): PeriodeDto? {
            if (periode == null) {
                return periode
            }
            return PeriodeDto(periode.fom, periode.tom)
        }
    }
}
