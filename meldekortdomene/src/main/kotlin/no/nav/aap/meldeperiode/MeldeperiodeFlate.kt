package no.nav.aap.meldeperiode

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.SakService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.sakServiceFactory
import no.nav.aap.utfylling.Svar
import java.time.LocalDate

class MeldeperiodeFlate(
    private val sakService: SakService?,
) {
    class KommendeMeldeperioder(
        val antallUbesvarteMeldeperioder: Int,
        val manglerOpplysninger: Periode?,
        val nesteMeldeperiode: Meldeperiode?,
    )
    fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker, dagensDato: LocalDate?): KommendeMeldeperioder {
        val ventendeOgNeste = sakService?.ventendeOgNesteMeldeperioder(innloggetBruker, dagensDato)
            ?: SakService.VentendeOgNeste(
                ventende = listOf(),
                neste = null,
            )

        val manglerOpplysninger = ventendeOgNeste.ventende
            .takeIf { it.isNotEmpty() }
            ?.let {
                Periode(
                    fom = it.first().meldeperioden.fom,
                    tom = it.last().meldeperioden.tom,
                )
            }
        return KommendeMeldeperioder(
            antallUbesvarteMeldeperioder = ventendeOgNeste.ventende.size,
            manglerOpplysninger = manglerOpplysninger,
            nesteMeldeperiode = ventendeOgNeste.ventende.firstOrNull() ?: ventendeOgNeste.neste
        )
    }

    class HistoriskMeldeperiode(
        val meldeperiode: Meldeperiode,
        val totaltAntallTimerIPerioden: Double,
    )
    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<HistoriskMeldeperiode> {
        return sakService?.historiskeMeldeperioder(innloggetBruker).orEmpty()
            .map {
                HistoriskMeldeperiode(
                    meldeperiode = it,
                    totaltAntallTimerIPerioden = totaltAntallTimerIPerioden(innloggetBruker, it.meldeperioden) ?: 0.0
                )
            }
    }

    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): SakService.PeriodeDetaljer {
        return sakService?.detaljer(innloggetBruker, periode)
            ?: SakService.PeriodeDetaljer(
                periode = periode,
                svar = Svar.tomt(periode),
            )
    }

    private fun totaltAntallTimerIPerioden(innloggetBruker: InnloggetBruker, periode: Periode): Double? {
        val detaljer = sakService?.detaljer(innloggetBruker, periode)
        if (detaljer !== null) {
            return sakService?.totaltAntallTimerArbeidet(detaljer)
        }
        return null
    }

    companion object {
        fun konstruer(ident: Ident, connection: DBConnection): MeldeperiodeFlate {
            val fagsystemService = SakerService.konstruer(connection).finnSak(ident, LocalDate.now())
                ?.let { sak -> sakServiceFactory(connection, sak) }
            return MeldeperiodeFlate(fagsystemService)
        }
    }
}