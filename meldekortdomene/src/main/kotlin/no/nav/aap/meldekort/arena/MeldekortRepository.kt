package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

interface MeldekortRepository {
    fun upsert(ident: Ident, meldekort: List<Meldekort>)
    fun upsert(ident: Ident, meldekort: Meldekort) {
        upsert(ident, listOf(meldekort))
    }

    fun hent(ident: Ident, meldekortId: Long): Meldekort?
    fun hent(ident: Ident, meldekortId: List<Long>): List<Meldekort>
    fun hentAlleHistoriskeMeldekort(ident: Ident): List<HistoriskMeldekort>
}