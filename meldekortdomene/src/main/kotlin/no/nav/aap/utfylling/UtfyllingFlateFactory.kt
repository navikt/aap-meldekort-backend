package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.kelvin.KelvinUtfyllingFlate
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock
import java.time.LocalDate

interface UtfyllingFlateFactory {
    fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, connection: DBConnection): Pair<String, UtfyllingFlate>
}

class UtfyllingFlateFactoryImpl(private val clock: Clock) : UtfyllingFlateFactory {
    val log = LoggerFactory.getLogger(javaClass)

    override fun flateForBruker(innloggetBruker: InnloggetBruker, repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, connection: DBConnection): Pair<String, UtfyllingFlate> {
        val sak = SakerService(repositoryProvider, gatewayProvider).finnSak(innloggetBruker.ident, LocalDate.now(clock))
        val saksnummer = sak?.referanse?.nummer?.asString ?: "NULL"

        MDC.putCloseable("saksnummer", saksnummer).use {
            log.info("Saksnummer '${saksnummer}' rutes til fagsystem ${sak?.referanse?.system?.name ?: "KELVIN"} for utfylling")
        }

        val flate = KelvinUtfyllingFlate(repositoryProvider, gatewayProvider, clock)

        return Pair(saksnummer, flate)
    }
}