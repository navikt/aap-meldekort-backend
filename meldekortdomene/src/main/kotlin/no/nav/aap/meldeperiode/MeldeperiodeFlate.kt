package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.fagsystemServiceFactory
import java.time.LocalDate

class MeldeperiodeFlate(
    val fagsystemService: FagsystemService?,
) {
    fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste? {
        return fagsystemService?.ventendeOgNesteMeldeperioder(innloggetBruker)
    }

    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode>? {
        return fagsystemService?.historiskeMeldeperioder(innloggetBruker)
    }

    fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer? {
        return fagsystemService?.detaljer(innloggetBruker, periode)
    }

    companion object {
        fun konstruer(innloggetBruker: InnloggetBruker, connection: DBConnection): MeldeperiodeFlate {
            val sak = SakerService.konstruer().finnSak(innloggetBruker, LocalDate.now())
                ?: return MeldeperiodeFlate(null)

            return MeldeperiodeFlate(
                fagsystemService = fagsystemServiceFactory(connection, sak)
            )
        }
    }
}