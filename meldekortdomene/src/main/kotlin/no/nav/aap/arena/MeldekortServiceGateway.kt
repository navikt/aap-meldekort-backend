package no.nav.aap.arena

import no.nav.aap.lookup.gateway.Gateway

interface MeldekortServiceGateway : Gateway {
    fun hentMeldekort(fnr: String): List<ArenaMeldekort>?
    fun hentHistoriskeMeldekort(fnr: String, antallMeldeperioder: Int = 10): List<ArenaMeldekort>?
}
