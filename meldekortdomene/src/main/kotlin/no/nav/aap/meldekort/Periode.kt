package no.nav.aap.meldekort

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : Iterable<LocalDate>, Comparable<Periode> {
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

    override fun compareTo(other: Periode): Int {
        val compare = fom.compareTo(other.fom)
        return if (compare != 0) compare else tom.compareTo(other.tom)
    }
}