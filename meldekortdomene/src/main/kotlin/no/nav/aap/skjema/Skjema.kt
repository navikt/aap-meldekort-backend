package no.nav.aap.skjema

import no.nav.aap.Ident
import no.nav.aap.Periode
import java.util.*
import no.nav.aap.arena.MeldekortId
import java.time.Instant
import java.time.LocalDateTime

/** Representerer noe medlemmet har sendt inn, enten
 * som utkast for mellomlagring, eller den endlige versjonen.
 */
data class Skjema(
    val tilstand: SkjemaTilstand,
    val meldekortId: MeldekortId,
    val ident: Ident,
    val meldeperiode: Periode,
    val svar: Svar,
    val referanse: UUID,
    val sendtInn: LocalDateTime?,
)

enum class SkjemaTilstand {
    UTKAST,
    FORSØKER_Å_SENDE_TIL_ARENA,
    SENDT_ARENA,
    JOURNALFØRT,
}