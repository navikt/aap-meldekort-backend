package no.nav.aap

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.AapGateway
import no.nav.aap.sak.Saker
import no.nav.aap.sak.SakerService

class AnsvarligFlate(
    private val sakerService: SakerService,
    private val aapGateway: AapGateway,
) {
    fun routingForBruker(innloggetBruker: InnloggetBruker): FagsystemNavn {
        /* Er det ATTF-meldegruppe i Arena? */
        /* TODO: mer logikk hvis det ikke er en åpen sak på dagens dato, men det:
         * 1. nylig har vært en sak,
         * 2. kommer en sak (er dette et mulig scenario?),
         * 3. både 1 og 2.
         */

        val fagsystem = sakerService.ansvarligFagsystem(innloggetBruker, LocalDate.now())
        return fagsystem ?: FagsystemNavn.ARENA
    }

    fun debugSaker(innloggetBruker: InnloggetBruker): Saker {
        return aapGateway.hentSaker(innloggetBruker.ident)
    }

    companion object {
        fun konstruer(connection: DBConnection): AnsvarligFlate {
            val aapGateway = GatewayProvider.provide<AapGateway>()
            return AnsvarligFlate(
                sakerService = SakerService(aapGateway, RepositoryProvider(connection).provide()),
                aapGateway = aapGateway,
            )
        }
    }
}