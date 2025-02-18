package no.nav.aap.sak

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaService
import no.nav.aap.kelvin.KelvinService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlyt
import no.nav.aap.utfylling.UtfyllingReferanse

enum class FagsystemNavn {
    ARENA, KELVIN,
}

interface FagsystemService {
    val sak: Sak
    val innsendingsflyt: UtfyllingFlyt
    val korrigeringsflyt: UtfyllingFlyt

    class VentendeOgNeste(
        val ventende: List<Meldeperiode>,
        val neste: Meldeperiode?,
    )
    fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): VentendeOgNeste

    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode>

    class PeriodeDetaljer(
        val periode: Periode,
        val svar: Svar,
    )
    fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): PeriodeDetaljer

    fun forberedVanligFlyt(innloggetBruker: InnloggetBruker, periode: Periode, utfyllingReferanse: UtfyllingReferanse)
    fun forberedKorrigeringFlyt(innloggetBruker: InnloggetBruker, periode: Periode, utfyllingReferanse: UtfyllingReferanse)

    fun utfyllingGyldig(utfylling: Utfylling): Boolean {
        return true
    }

    fun tomtSvar(periode: Periode): Svar {
        return Svar.tomt(periode)
    }

    fun hentHistoriskeSvar(innloggetBruker: InnloggetBruker, periode: Periode): Svar {
        return Svar.tomt(periode)
    }
}

fun fagsystemServiceFactory(connection: DBConnection, sak: Sak): FagsystemService {
    return when (sak.fagsystemNavn) {
        FagsystemNavn.ARENA -> ArenaService.konstruer(connection, sak)
        FagsystemNavn.KELVIN -> KelvinService.konstruer(connection, sak)
    }
}