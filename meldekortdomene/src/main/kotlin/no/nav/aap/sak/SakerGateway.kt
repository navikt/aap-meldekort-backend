package no.nav.aap.sak

import no.nav.aap.InnloggetBruker
import no.nav.aap.lookup.gateway.Gateway

interface SakerGateway: Gateway {
    fun hentSaker(innloggetBruker: InnloggetBruker): Saker
}