package no.nav.aap.sak

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.lookup.gateway.Gateway

interface AapGateway: Gateway {
    fun hentSaker(innloggetBruker: InnloggetBruker): Saker

    fun hentMeldeperioder(innloggetBruker: InnloggetBruker, periode: Periode): List<Periode>
}