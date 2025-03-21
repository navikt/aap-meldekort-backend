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
    fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): SakService.VentendeOgNeste {
        return sakService?.ventendeOgNesteMeldeperioder(innloggetBruker)
            ?: SakService.VentendeOgNeste(
                ventende = listOf(),
                neste = null,
            )
    }

    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        return sakService?.historiskeMeldeperioder(innloggetBruker)
            ?: listOf()
    }

    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): SakService.PeriodeDetaljer {
        return sakService?.detaljer(innloggetBruker, periode)
            ?: SakService.PeriodeDetaljer(
                periode = periode,
                svar = Svar.tomt(periode),
            )
    }

    fun totaltAntallTimerIPerioden(innloggetBruker: InnloggetBruker, periode: Periode): Double? {
        val detaljer = sakService?.detaljer(innloggetBruker, periode)
        if(detaljer !== null) {
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