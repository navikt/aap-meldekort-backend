package no.nav.aap.arena

import no.nav.aap.utfylling.Utfylling

data class ArenaInnsendingFeiletException(
    val kontrollStatus: String,
    val innsendingFeil: List<InnsendingFeil>,
    val skjema: Utfylling? = null,
) : Exception("kontrollStatus=${kontrollStatus} feil=${innsendingFeil.joinToString { it.kode }}") {
    data class InnsendingFeil(
        val kode: String,
        val params: List<String>? = null
    )
}
