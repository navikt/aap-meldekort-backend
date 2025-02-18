package no.nav.aap.meldeperiode

import no.nav.aap.Periode
import java.time.LocalDate

class Meldeperiode(
    val meldeperioden: Periode,

    /** Dager hvor det å melde seg vil påvirke vurderingen av meldeplikten for denne perioden. */
    val meldevindu: Periode,
) {

}

