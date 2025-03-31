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
    fun routingForBrukerHosOss(innloggetBruker: InnloggetBruker): FagsystemNavn {
        val fagsystem = sakerService.ansvarligFagsystem(innloggetBruker, LocalDate.now())
        return fagsystem ?: FagsystemNavn.KELVIN
    }

    fun routingForBrukerHosFelles(innloggetBruker: InnloggetBruker): FagsystemNavn {
        val kelvinSak = sakerService.finnKelvinSak(innloggetBruker, LocalDate.now())
        return if (kelvinSak == null) FagsystemNavn.ARENA else FagsystemNavn.KELVIN
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