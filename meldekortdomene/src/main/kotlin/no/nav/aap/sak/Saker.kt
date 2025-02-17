package no.nav.aap.sak

import no.nav.aap.meldeplikt.Medlem
import java.time.LocalDate

class Saker(
    val saker: List<Sak>,
) {
    /* Burde vi oppfordre medlemmet til å melde seg i dag? */
    fun burdeMeldeSeg(medlemmet: Medlem, idag: LocalDate): Boolean {
        val sak = finnSakForDagen(idag) ?: return false
        return sak.burdeMeldeSeg(medlemmet, idag)
    }

    fun finnSakForDagen(idag: LocalDate): Sak? {
        return saker.filter { idag in it.rettighetsperiode }
            .also {
                check(it.size <= 1) {
                    "medlemmet har saker med overlappende rettighetsperioder: ${it.joinToString(", ") { it.saksnummer?.asString ?: "?" }}"
                }
            }
            .singleOrNull()
    }
}
