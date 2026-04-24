package no.nav.aap.arena

import java.time.LocalDate
import java.time.LocalDateTime

data class ArenaMeldekort(
    val meldekortId: Long,
    val kortType: String,
    val meldeperiode: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val hoyesteMeldegruppe: String,
    val beregningstatus: String,
    val forskudd: Boolean,
    val mottattDato: LocalDate? = null,
    val bruttoBelop: Float = 0F,
) {
    fun erAAPMeldekort(): Boolean = hoyesteMeldegruppe == AAP

    // Åpnes for innsending fra midnatt siste lørdag
    fun kanSendesFra(): LocalDateTime = tilDato.minusDays(1).atStartOfDay()

    companion object {
        private const val AAP = "AAP"
    }
}
