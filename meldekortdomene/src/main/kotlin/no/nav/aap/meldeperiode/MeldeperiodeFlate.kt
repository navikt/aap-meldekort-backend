package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.fagsystemServiceFactory
import no.nav.aap.utfylling.Svar
import java.time.LocalDate

class MeldeperiodeFlate(
    val fagsystemService: FagsystemService?,
) {
    fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        return fagsystemService?.ventendeOgNesteMeldeperioder(innloggetBruker)
            ?: FagsystemService.VentendeOgNeste(
                ventende = listOf(),
                neste = null,
            )
    }

    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        return fagsystemService?.historiskeMeldeperioder(innloggetBruker)
            ?: listOf()
    }

    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer {
        return fagsystemService?.detaljer(innloggetBruker, periode)
            ?: FagsystemService.PeriodeDetaljer(
                periode = periode,
                svar = Svar.tomt(periode),
            )
    }

    companion object {
        fun konstruer(innloggetBruker: InnloggetBruker, connection: DBConnection): MeldeperiodeFlate {
            val fagsystemService = SakerService.konstruer().finnSak(innloggetBruker, LocalDate.now())
                ?.let { sak -> fagsystemServiceFactory(connection, sak) }
            return MeldeperiodeFlate(fagsystemService)
        }
    }
}