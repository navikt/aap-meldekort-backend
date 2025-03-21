package no.nav.aap.sak

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SakerService(
    private val aapGateway: AapGateway,
    private val kelvinSakRepository: KelvinSakRepository,
) {
    fun finnSak(innloggetBruker: InnloggetBruker, påDag: LocalDate): Sak? {
        return finnSak(innloggetBruker.ident, påDag)
    }

    fun finnSak(ident: Ident, påDag: LocalDate): Sak? {
        val kelvinSak = kelvinSakRepository.hentSak(ident, påDag)
        if (kelvinSak != null) {
            return kelvinSak
        }

        val saker = aapGateway.hentSaker(ident)
        return saker.finnSakForDagen(påDag)
    }

    fun finnSak(ident: Ident, fagsakReferanse: FagsakReferanse): Sak? {
        val saker = aapGateway.hentSaker(ident)
        return saker.finnSak(fagsakReferanse)
    }

    fun ansvarligFagsystem(innloggetBruker: InnloggetBruker, påDag: LocalDate): FagsystemNavn? {
        return finnSak(innloggetBruker, påDag)?.referanse?.system
    }

    companion object {
        fun konstruer(connection: DBConnection): SakerService {
            return SakerService(
                GatewayProvider.provide<AapGateway>(),
                RepositoryProvider(connection).provide(),
            )
        }
    }
}