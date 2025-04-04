package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaMeldeperiodeFlate
import no.nav.aap.kelvin.KelvinMeldeperiodeFlate
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.LocalDate
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider

interface MeldeperiodeFlateFactory {
    fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): MeldeperiodeFlate
}

class MeldeperiodeFlateFactoryImpl: MeldeperiodeFlateFactory {
    override fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): MeldeperiodeFlate {
        val sak = SakerService(repositoryProvider,gatewayProvider).finnSak(innloggetBruker.ident, LocalDate.now())
        return when (sak?.referanse?.system) {
            null, FagsystemNavn.KELVIN -> KelvinMeldeperiodeFlate(repositoryProvider, gatewayProvider)
            FagsystemNavn.ARENA -> ArenaMeldeperiodeFlate(repositoryProvider)
        }
    }
}