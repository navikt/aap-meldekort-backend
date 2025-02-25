package no.nav.aap.sak

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.lookup.gateway.Gateway

interface AapGateway: Gateway {
    fun hentSaker(ident: Ident): Saker
    fun hentMeldeperioder(ident: Ident, periode: Periode): List<Periode>
}