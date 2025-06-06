package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.komponenter.repository.Repository

interface MeldekortRepository: Repository {
    fun upsert(ident: Ident, meldekort: List<Meldekort>)
    fun upsert(ident: Ident, meldekort: Meldekort) {
        upsert(ident, listOf(meldekort))
    }

    fun hent(ident: Ident, meldekortId: MeldekortId): Meldekort?
    fun hent(ident: Ident, meldekortId: List<MeldekortId>): List<Meldekort>
    fun hentAlleHistoriskeMeldekort(ident: Ident): List<HistoriskMeldekort>
}