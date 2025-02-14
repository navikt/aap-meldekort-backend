package no.nav.aap

import no.nav.aap.sak.Fagsystem
import no.nav.aap.sak.SakerGateway
import no.nav.aap.sak.Saker

class AnsvarligFlate(
    private val sakerGateway: SakerGateway,
) {
    fun aktivtFagsystem(innloggetBruker: InnloggetBruker): Fagsystem {
        return Fagsystem.KELVIN
    }

    fun debugSaker(innloggetBruker: InnloggetBruker): Saker {
        return sakerGateway.hentSaker(innloggetBruker)
    }

}