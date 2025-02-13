package no.nav.aap.meldekort.arena

import no.nav.aap.Ident
import no.nav.aap.Periode
import java.time.LocalDateTime
import java.util.*

data class Skjema(
    val tilstand: SkjemaTilstand,
    val meldekortId: MeldekortId,
    val ident: Ident,
    val meldeperiode: Periode,
    val payload: InnsendingPayload,
    val referanse: UUID,
    val sendtInn: LocalDateTime?,
)

enum class SkjemaTilstand {
    UTKAST,
    FORSØKER_Å_SENDE_TIL_ARENA,
    SENDT_ARENA,
    JOURNALFØRT,
}