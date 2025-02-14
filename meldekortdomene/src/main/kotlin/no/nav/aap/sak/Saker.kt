package no.nav.aap.sak

import no.nav.aap.meldeplikt.Medlem
import java.time.LocalDate

class Saker(
    private val saker: List<Sak>,
) {
    /* Burde vi oppfordre medlemmet til å melde seg i dag? */
    fun burdeMeldeSeg(medlemmet: Medlem, idag: LocalDate): Boolean {
        val sak = finnRelevantSak(idag) ?: return false
        return sak.burdeMeldeSeg(medlemmet, idag)
    }

    private fun finnRelevantSak(idag: LocalDate): Sak? {
        return saker.filter { idag in it.rettighetsperiode }
            .also {
                check(it.size <= 1) {
                    "medlemmet har saker med overlappende rettighetsperioder: ${it.joinToString(", ") { it.saksnummer.asString }}"
                }
            }
            .singleOrNull()
    }
}
