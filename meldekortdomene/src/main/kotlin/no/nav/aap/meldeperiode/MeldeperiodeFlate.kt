package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.sak.SakService
import java.time.LocalDate

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

    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): SakService.PeriodeDetaljer
}
