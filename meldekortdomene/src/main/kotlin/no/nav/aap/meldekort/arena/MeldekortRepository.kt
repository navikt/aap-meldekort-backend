package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

interface MeldekortRepository {
    fun oppdater(ident: Ident, meldekort: Meldekort)
    fun upsertFraArena(ident: Ident, meldekort: List<Meldekort>)

    fun hent(ident: Ident, meldekortId: Long): Meldekort?
    fun hent(ident: Ident, meldekortId: List<Long>): List<Meldekort>
    fun hentAlleHistoriskeMeldekort(ident: Ident): List<HistoriskMeldekort>
}