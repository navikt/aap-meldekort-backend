package no.nav.aap.arena

import no.nav.aap.sak.FagsystemService
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.ArenaKorrigeringFlyt
import no.nav.aap.utfylling.ArenaVanligFlyt
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.Utfyllingsflyter

class ArenaService(): FagsystemService {
    override val innsendingsflyt = ArenaVanligFlyt(this)
    override val korrigeringsflyt = ArenaKorrigeringFlyt(this)

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        return FagsystemService.VentendeOgNeste(emptyList(), null)
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        return emptyList()
    }

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer {
        return FagsystemService.PeriodeDetaljer(periode, svar = tomtSvar(periode))
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

    fun sendInnKorrigering(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    fun sendInnVanlig(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    companion object {
        fun konstruer(connection: DBConnection): ArenaService {
            return ArenaService()
        }
    }
}