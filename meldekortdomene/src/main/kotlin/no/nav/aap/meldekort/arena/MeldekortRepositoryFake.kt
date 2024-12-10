package no.nav.aap.meldekort.arena

class MeldekortRepositoryFake: MeldekortRepository {
    private val innsendteMeldekort: MutableList<InnsendtMeldekort> = mutableListOf()

    override fun storeMeldekort(meldekort: InnsendtMeldekort): InnsendtMeldekort {
        innsendteMeldekort.add(meldekort)
        return meldekort
    }

    override fun loadMeldekort(): List<InnsendtMeldekort> {
        return innsendteMeldekort
    }
}