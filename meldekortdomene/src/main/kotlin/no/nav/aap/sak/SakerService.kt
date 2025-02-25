package no.nav.aap.sak

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.lookup.gateway.GatewayProvider
import java.time.LocalDate

class SakerService(
    private val aapGateway: AapGateway,
) {
    fun finnSak(innloggetBruker: InnloggetBruker, påDag: LocalDate): Sak? {
        return finnSak(innloggetBruker.ident, påDag)
    }

    fun finnSak(ident: Ident, påDag: LocalDate): Sak? {
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
        fun konstruer(): SakerService {
            return SakerService(GatewayProvider.provide<AapGateway>())
        }
    }
}