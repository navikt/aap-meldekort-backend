package no.nav.aap.sak

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaSakService
import no.nav.aap.journalføring.DokarkivGateway
import no.nav.aap.kelvin.KelvinSakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse

enum class FagsystemNavn {
    ARENA, KELVIN,
}

interface SakService {
    val sak: Sak
    val innsendingsflyt: UtfyllingFlytNavn
    val korrigeringsflyt: UtfyllingFlytNavn

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

    fun hentHistoriskeSvar(innloggetBruker: InnloggetBruker, periode: Periode): Svar

    class OpplysningerForJournalpost(
        val tittel: String,
        val brevkode: String,
        val journalførPåSak: DokarkivGateway.Sak?,
        val ferdigstill: Boolean,
        val tilleggsopplysning: Map<String, String>,
    )
    fun opplysningerForJournalpost(utfylling: Utfylling): OpplysningerForJournalpost
}

fun sakServiceFactory(connection: DBConnection, sak: Sak): SakService {
    return when (sak.referanse.system) {
        FagsystemNavn.ARENA -> ArenaSakService.konstruer(connection, sak)
        FagsystemNavn.KELVIN -> KelvinSakService.konstruer(connection, sak)
    }
}