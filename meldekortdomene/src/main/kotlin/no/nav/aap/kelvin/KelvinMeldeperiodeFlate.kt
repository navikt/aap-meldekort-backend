package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.sak.SakService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.sakServiceFactory
import no.nav.aap.utfylling.Svar
import java.time.LocalDate

class KelvinMeldeperiodeFlate(
    private val sakService: SakService?,
) : MeldeperiodeFlate {
    override fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): MeldeperiodeFlate.KommendeMeldeperioder {
        val ventendeOgNeste = sakService?.ventendeOgNesteMeldeperioder(innloggetBruker)
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
        return MeldeperiodeFlate.KommendeMeldeperioder(
            antallUbesvarteMeldeperioder = ventendeOgNeste.ventende.size,
            manglerOpplysninger = manglerOpplysninger,
            nesteMeldeperiode = ventendeOgNeste.ventende.firstOrNull() ?: ventendeOgNeste.neste
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<MeldeperiodeFlate.HistoriskMeldeperiode> {
        return sakService?.historiskeMeldeperioder(innloggetBruker).orEmpty()
            .map {
                MeldeperiodeFlate.HistoriskMeldeperiode(
                    meldeperiode = it,
                    totaltAntallTimerIPerioden = totaltAntallTimerIPerioden(innloggetBruker, it.meldeperioden) ?: 0.0
                )
            }
    }

    override fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): MeldeperiodeFlate.PeriodeDetaljer {
        return sakService?.detaljer(innloggetBruker, periode)
            ?: MeldeperiodeFlate.PeriodeDetaljer(
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
        fun konstruer(ident: Ident, connection: DBConnection): KelvinMeldeperiodeFlate {
            val fagsystemService = SakerService.konstruer(connection).finnSak(ident, LocalDate.now())
                ?.let { sak -> sakServiceFactory(connection, sak) }
            return KelvinMeldeperiodeFlate(fagsystemService)
        }
    }
}
