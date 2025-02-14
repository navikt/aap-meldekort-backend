package no.nav.aap.meldekort.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.kelvin.KelvinGateway
import no.nav.aap.sak.Saker

class KelvinGatewayImpl(
    val baseUrl: String,
    val scope: String,
): KelvinGateway {

    override fun hentSaker(innloggetBruker: InnloggetBruker): Saker {
        TODO("Not yet implemented")
    }
}