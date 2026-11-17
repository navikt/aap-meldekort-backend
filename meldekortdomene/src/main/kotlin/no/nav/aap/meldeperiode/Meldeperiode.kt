package no.nav.aap.meldeperiode

import no.nav.aap.Periode

data class Meldeperiode(
    val meldeperioden: Periode,

    /** Dager hvor det å melde seg vil påvirke vurderingen av meldeplikten for denne perioden. */
    val meldevindu: Periode,
)

fun Meldeperiode.erDekketAvFritak(fritak: List<Periode>): Boolean =
    fritak.isNotEmpty() && meldeperioden.all { dag -> fritak.any { dag in it } }
