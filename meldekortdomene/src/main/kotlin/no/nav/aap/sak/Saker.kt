package no.nav.aap.sak

import org.slf4j.LoggerFactory
import java.time.LocalDate

class Saker(
    private val saker: List<Sak>,
) {
    val log = LoggerFactory.getLogger(javaClass)

//    /* Burde vi oppfordre medlemmet til å melde seg i dag? */
//    fun burdeMeldeSeg(medlemmet: Medlem, idag: LocalDate): Boolean {
//        val sak = finnSakForDagen(idag) ?: return false
//        return sak.burdeMeldeSeg(medlemmet, idag)
//    }

    fun finnSakForDagen(idag: LocalDate): Sak? {
        return saker.filter { idag in it.rettighetsperiode }
            .also { sakerMedRettighetsperiodePåDato ->
                if (sakerMedRettighetsperiodePåDato.size > 1) {
                    if (gjelderKelvin(sakerMedRettighetsperiodePåDato)) {
                        throw IllegalStateException(lagFeilmelding(sakerMedRettighetsperiodePåDato))
                    }
                    log.info(lagFeilmelding(sakerMedRettighetsperiodePåDato))
                }
            }
            .maxByOrNull { it.rettighetsperiode.tom }
    }

    private fun lagFeilmelding(saker: List<Sak>): String =
        "medlemmet har saker med overlappende rettighetsperioder: ${saker.joinToString(", ") { it.referanse.toString() }}"

    private fun gjelderKelvin(sakerMedRettighetsperiodePåDato: List<Sak>): Boolean =
        sakerMedRettighetsperiodePåDato.any { saker -> saker.referanse.system == FagsystemNavn.KELVIN }

    fun finnSak(fagsakReferanse: FagsakReferanse): Sak? {
        return saker.firstOrNull { it.referanse == fagsakReferanse }
    }
}
