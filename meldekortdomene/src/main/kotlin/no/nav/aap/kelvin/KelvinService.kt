package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.AapFlyt
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate

class KelvinService(
    override val sak: Sak,
) : FagsystemService {
    override val innsendingsflyt = AapFlyt
    override val korrigeringsflyt = AapFlyt

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        /* TODO: behandlingsflyt bestemmer hva meldeperiodene er. */
        val perioder = Periode(sak.rettighetsperiode.fom, LocalDate.now()).slidingWindow(size = 14, step = 14, partialWindows = true)
            .map { Meldeperiode(
                meldeperioden = it,
                meldevindu = Periode(it.tom.plusDays(1), it.tom.plusDays(8)),
            )}

        /* TODO: mye å gjøre */
        val tidligereMeldeperioder = perioder.filter { it.meldevindu.tom <= LocalDate.now() }
        return FagsystemService.VentendeOgNeste(
            ventende = tidligereMeldeperioder,
            neste = tidligereMeldeperioder.firstOrNull(),
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val perioder = Periode(sak.rettighetsperiode.fom, LocalDate.now()).slidingWindow(size = 14, step = 14, partialWindows = true)
            .map { Meldeperiode(
                meldeperioden = it,
                meldevindu = Periode(it.tom.plusDays(1), it.tom.plusDays(8)),
            )}

        /* TODO: mye å gjøre */
        return perioder.filter { it.meldevindu.tom <= LocalDate.now() }
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
        fun konstruer(connection: DBConnection, sak: Sak): KelvinService {
            return KelvinService(sak)
        }
    }
}
