package no.nav.aap.meldekort.arena

import no.nav.aap.Ident
import no.nav.aap.arena.HistoriskMeldekort
import no.nav.aap.arena.Meldekort
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortRepository

class MeldekortRepositoryFake: MeldekortRepository {
    private val store: MutableMap<Pair<Ident, MeldekortId>, Meldekort> = mutableMapOf()

    fun oppdater(ident: Ident, meldekort: Meldekort) {
        store[ident to meldekort.meldekortId] = meldekort
    }

    override fun hent(ident: Ident, meldekortId: MeldekortId): Meldekort? {
        return store[ident to meldekortId]
    }

    override fun hent(ident: Ident, meldekortId: List<MeldekortId>): List<Meldekort> {
        return store.entries
            .asSequence()
            .filter { it.key.first == ident && it.key.second in meldekortId}
            .map { it.value }
            .toList()
    }

    override fun hentAlleHistoriskeMeldekort(ident: Ident): List<HistoriskMeldekort> {
        return store.entries.asSequence()
            .filter { it.key.first == ident }
            .map { it.value }
            .filterIsInstance<HistoriskMeldekort>()
            .toList()
    }

    override fun upsert(ident: Ident, meldekort: List<Meldekort>) {
        meldekort.forEach {
            oppdater(ident, it)
        }
    }
}