package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.sak.Saker

interface KelvinGateway: Gateway {
    fun hentSaker(innloggetBruker: InnloggetBruker): Saker
}