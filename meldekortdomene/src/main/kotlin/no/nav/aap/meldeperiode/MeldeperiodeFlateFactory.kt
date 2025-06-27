package no.nav.aap.meldeperiode

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaMeldeperiodeFlate
import no.nav.aap.kelvin.KelvinMeldeperiodeFlate
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.LocalDate
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock

interface MeldeperiodeFlateFactory {
    fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): Pair<String, MeldeperiodeFlate>
}

class MeldeperiodeFlateFactoryImpl(private val clock: Clock): MeldeperiodeFlateFactory {
    val log = LoggerFactory.getLogger(javaClass)

    override fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): Pair<String, MeldeperiodeFlate> {
        val sak = SakerService(repositoryProvider,gatewayProvider).finnSak(innloggetBruker.ident, LocalDate.now(clock))
        val saksnummer = sak?.referanse?.nummer?.asString ?: "NULL"

        MDC.putCloseable("saksnummer", saksnummer).use {
            log.info("Saksnummer '${saksnummer}' rutes til fagsystem ${sak?.referanse?.system?.name ?: "KELVIN"} for utfylling")
        }

        val flate = when (sak?.referanse?.system) {
            null, FagsystemNavn.KELVIN -> KelvinMeldeperiodeFlate(repositoryProvider, gatewayProvider, clock)
            FagsystemNavn.ARENA -> ArenaMeldeperiodeFlate(repositoryProvider)
        }

        return Pair(saksnummer, flate)
    }
}