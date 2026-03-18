package no.nav.aap

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : Iterable<LocalDate>, Comparable<Periode> {
    init {
        require(fom <= tom) { "Tom er før fom: $fom, $tom" }
    }

    override fun iterator(): Iterator<LocalDate> {
        return fom.datesUntil(tom.plusDays(1)).iterator()
    }

    operator fun contains(dato: LocalDate): Boolean {
        return dato in fom..tom
    }

    fun overlapper(other: Periode): Boolean {
        return this.fom <= other.tom && other.fom <= this.tom
    }

    override fun compareTo(other: Periode): Int {
        val compare = fom.compareTo(other.fom)
        return if (compare != 0) compare else tom.compareTo(other.tom)
    }

    fun slidingWindow(size: Int, step: Int = 1, partialWindows: Boolean = false): List<Periode> {
        /* ikke så effektivt ... */
        return this.windowed(size, step, partialWindows).map { Periode(it.first(), it.last()) }
    }
}