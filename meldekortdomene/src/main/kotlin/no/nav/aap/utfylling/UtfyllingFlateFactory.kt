package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaUtfyllingFlate
import no.nav.aap.kelvin.KelvinUtfyllingFlate
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.Clock
import java.time.LocalDate

interface UtfyllingFlateFactory {
    fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, connection: DBConnection): UtfyllingFlate
}

class UtfyllingFlateFactoryImpl(private val clock: Clock) : UtfyllingFlateFactory {
    override fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, connection: DBConnection): UtfyllingFlate {
        val sak = SakerService(repositoryProvider, gatewayProvider).finnSak(innloggetBruker.ident, LocalDate.now(clock))
        return when (sak?.referanse?.system) {
            null, FagsystemNavn.KELVIN -> KelvinUtfyllingFlate(repositoryProvider, gatewayProvider, clock)
            FagsystemNavn.ARENA -> ArenaUtfyllingFlate(repositoryProvider)
        }
    }
}