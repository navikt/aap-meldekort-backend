package no.nav.aap.meldekort.kontrakt

import java.time.LocalDate

public data class Periode(
    public val fom: LocalDate,
    public val tom: LocalDate,
) {
    init {
        require(fom <= tom)
    }
}