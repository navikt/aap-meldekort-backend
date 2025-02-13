package no.nav.aap.meldeplikt

import no.nav.aap.Periode
import java.time.LocalDate

class Meldeperiode(
    val meldeperioden: Periode,
    val fastsattDag: LocalDate,

    /** Dager hvor det å melde seg vil påvirke vurderingen av meldeplikten for denne perioden. */
    val meldevindu: Periode,
) {

    fun åMeldeSegHarEnEffekt(sistMeldt: LocalDate, idag: LocalDate): Boolean {
        require(sistMeldt <= idag)
        return kanMeldeSeg(idag) && !erAlleredeMeldt(sistMeldt)
    }

    fun erForSentÅMeldeSeg(idag: LocalDate): Boolean {
        return meldevindu.tom < idag
    }

    fun kanMeldeSeg(idag: LocalDate): Boolean {
        return idag in meldevindu
    }

    fun erAlleredeMeldt(sistMeldt: LocalDate): Boolean {
        return meldevindu.fom <= sistMeldt
    }
}

