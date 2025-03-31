package no.nav.aap

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : Iterable<LocalDate>, Comparable<Periode> {
    init {
        require(fom <= tom) { "Tom er før fom: $fom, $tom" }
    }

    val antallDager: Int
        get() = fom.until(tom.plusDays(1), ChronoUnit.DAYS).toInt()

    override fun iterator(): Iterator<LocalDate> {
        return fom.datesUntil(tom.plusDays(1)).iterator()
    }

    operator fun contains(dato: LocalDate): Boolean {
        return dato in fom..tom
    }

    fun overlapper(other: Periode): Boolean {
        return this.fom <= other.tom && other.fom <= this.tom
    }

    enum class Klassifikasjon {
        DAGEN_ER_FØR_PERIODEN, DAGER_ER_I_PERIODEN, DAGEN_ER_ETTER_PERIODEN
    }

    fun klassifiser(dag: LocalDate): Klassifikasjon {
        return when {
            dag < fom -> Klassifikasjon.DAGEN_ER_FØR_PERIODEN
            tom < dag -> Klassifikasjon.DAGEN_ER_ETTER_PERIODEN
            else -> Klassifikasjon.DAGER_ER_I_PERIODEN
        }
    }

    override fun compareTo(other: Periode): Int {
        val compare = fom.compareTo(other.fom)
        return if (compare != 0) compare else tom.compareTo(other.tom)
    }

    fun slidingWindow(size: Int, step: Int = 1, partialWindows: Boolean = false): List<Periode> {
        /* ikke så effektivt ... */
        return this.windowed(size, step, partialWindows).map { Periode(it.first(), it.last()) }
    }

    companion object {
        fun snitt(perioder: List<Periode>): Periode? {
            val fom = perioder.maxOfOrNull { it.fom }
            val tom = perioder.minOfOrNull { it.tom }
            return if (fom != null && tom != null && fom <= tom) {
                Periode(fom, tom)
            } else {
                null
            }
        }
    }
}