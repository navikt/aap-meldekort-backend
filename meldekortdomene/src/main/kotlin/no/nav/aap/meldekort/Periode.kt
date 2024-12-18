package no.nav.aap.meldekort

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : Iterable<LocalDate> {
    init {
        require(fom <= tom)
    }

    val antallDager: Int
        get() = fom.until(tom.plusDays(1), ChronoUnit.DAYS).toInt()

    override fun iterator(): Iterator<LocalDate> {
        return fom.datesUntil(tom.plusDays(1)).iterator()
    }

    fun inneholder(dato: LocalDate): Boolean {
        return dato in fom..tom
    }
}