package no.nav.aap.sak

import no.nav.aap.InnloggetBruker
import no.nav.aap.lookup.gateway.GatewayProvider
import java.time.LocalDate

class SakerService(
    private val sakerGateway: SakerGateway,
) {
    fun finnSak(innloggetBruker: InnloggetBruker, påDag: LocalDate): Sak? {
        val saker = sakerGateway.hentSaker(innloggetBruker)
        return saker.finnSakForDagen(påDag)
    }

    fun ansvarligFagsystem(innloggetBruker: InnloggetBruker, påDag: LocalDate): FagsystemNavn? {
        return finnSak(innloggetBruker, påDag)?.fagsystemNavn
    }

    companion object {
        fun konstruer(): SakerService {
            return SakerService(GatewayProvider.provide<SakerGateway>())

        }
    }
}