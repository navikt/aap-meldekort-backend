package no.nav.aap.meldekort.arena

interface MeldekortRepository {
    fun storeMeldekort(meldekort: InnsendtMeldekort): InnsendtMeldekort
    fun loadMeldekort(): List<InnsendtMeldekort>
}