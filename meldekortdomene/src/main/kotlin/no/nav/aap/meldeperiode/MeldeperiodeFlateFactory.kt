package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaMeldeperiodeFlate
import no.nav.aap.kelvin.KelvinMeldeperiodeFlate
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.LocalDate

interface MeldeperiodeFlateFactory {
    fun flateForBruker(innloggetBruker: InnloggetBruker, connection: DBConnection): MeldeperiodeFlate
}

class MeldeperiodeFlateFactoryImpl: MeldeperiodeFlateFactory {
    override fun flateForBruker(innloggetBruker: InnloggetBruker, connection: DBConnection): MeldeperiodeFlate {
        val sak = SakerService.konstruer(connection).finnSak(innloggetBruker.ident, LocalDate.now())
        return when (sak?.referanse?.system) {
            null, FagsystemNavn.KELVIN -> KelvinMeldeperiodeFlate.konstruer(connection)
            FagsystemNavn.ARENA -> ArenaMeldeperiodeFlate.konstruer(connection)
        }
    }
}