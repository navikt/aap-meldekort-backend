package no.nav.aap.sak

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import java.time.LocalDate

class SakerService(
    private val aapGateway: AapGateway,
    private val kelvinSakRepository: KelvinSakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        aapGateway = gatewayProvider.provide(),
        kelvinSakRepository = repositoryProvider.provide(),
    )

    fun finnSak(innloggetBruker: InnloggetBruker, påDag: LocalDate): Sak? {
        return finnSak(innloggetBruker.ident, påDag)
    }

    fun finnSak(ident: Ident, påDag: LocalDate): Sak? {
        val kelvinSak = kelvinSakRepository.hentSak(ident, påDag)
        if (kelvinSak != null && påDag in kelvinSak.rettighetsperiode) {
            return kelvinSak
        }

        val saker = aapGateway.hentSaker(ident)
        return saker.finnSakForDagen(påDag)
    }

    fun finnSak(ident: Ident, fagsakReferanse: FagsakReferanse): Sak? {
        val saker = aapGateway.hentSaker(ident)
        return saker.finnSak(fagsakReferanse)
    }
}