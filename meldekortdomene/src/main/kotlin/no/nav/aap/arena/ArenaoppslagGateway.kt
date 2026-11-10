package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.sak.Saker

interface ArenaoppslagGateway : Gateway {
    fun hentSaker(ident: Ident): Saker
}
