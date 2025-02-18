package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.FagsystemService
import no.nav.aap.utfylling.AapFlyt
import no.nav.aap.utfylling.UtfyllingReferanse

class KelvinService : FagsystemService {
    override val innsendingsflyt = AapFlyt
    override val korrigeringsflyt = AapFlyt

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        return FagsystemService.VentendeOgNeste(
            ventende = listOf(),
            neste = null,
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        return listOf()
    }

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer {
        return FagsystemService.PeriodeDetaljer(periode, tomtSvar(periode))
    }

    override fun forberedVanligFlyt(
        innloggetBruker: InnloggetBruker,
        periode: Periode,
        utfyllingReferanse: UtfyllingReferanse
    ) {
    }

    override fun forberedKorrigeringFlyt(
        innloggetBruker: InnloggetBruker,
        periode: Periode,
        utfyllingReferanse: UtfyllingReferanse
    ) {
    }

    companion object {
        fun konstruer(connection: DBConnection): KelvinService {
            return KelvinService()
        }
    }
}
