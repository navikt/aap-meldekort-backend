package no.nav.aap.meldeperiode

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.utfylling.Svar

interface MeldeperiodeFlate {
    class KommendeMeldeperioder(
        val antallUbesvarteMeldeperioder: Int,
        val manglerOpplysninger: Periode?,
        val nesteMeldeperiode: Meldeperiode?,
    )
    fun aktuelleMeldeperioder(ident: Ident): KommendeMeldeperioder

    class HistoriskMeldeperiode(
        val meldeperiode: Meldeperiode,
        val totaltAntallTimerIPerioden: Double,
    )
    fun historiskeMeldeperioder(ident: Ident): List<HistoriskMeldeperiode>

    class PeriodeDetaljer(
        val periode: Periode,
        val svar: Svar,
    )
    fun periodedetaljer(ident: Ident, periode: Periode): PeriodeDetaljer
}
