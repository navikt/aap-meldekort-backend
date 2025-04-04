package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.utfylling.Svar

interface MeldeperiodeFlate {
    class KommendeMeldeperioder(
        val antallUbesvarteMeldeperioder: Int,
        val manglerOpplysninger: Periode?,
        val nesteMeldeperiode: Meldeperiode?,
    )
    fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): KommendeMeldeperioder

    class HistoriskMeldeperiode(
        val meldeperiode: Meldeperiode,
        val totaltAntallTimerIPerioden: Double,
    )
    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<HistoriskMeldeperiode>

    class PeriodeDetaljer(
        val periode: Periode,
        val svar: Svar,
    )
    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): PeriodeDetaljer
}
