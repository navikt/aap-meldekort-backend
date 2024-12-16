package no.nav.aap.meldekort.arena

class ArenaInnsendingFeiletException(
    val innsendingFeil: List<InnsendingFeil>
): Exception() {
    data class InnsendingFeil(
        val kode: String,
        val params: List<String>? = null
    )
}
