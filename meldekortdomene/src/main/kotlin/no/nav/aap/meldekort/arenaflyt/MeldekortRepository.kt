package no.nav.aap.meldekort.arenaflyt

interface MeldekortRepository {
    fun storeMeldekort(meldekort: InnsendtMeldekort): InnsendtMeldekort
    fun loadMeldekort(): List<InnsendtMeldekort>
}