package no.nav.aap.meldekort.arena

data class ArenaInnsendingFeiletException(
    val innsendingFeil: List<InnsendingFeil>,
    val skjema: Skjema? = null,
): Exception() {
    data class InnsendingFeil(
        val kode: String,
        val params: List<String>? = null
    )
}
