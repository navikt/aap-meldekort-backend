package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.Periode
import java.time.LocalDateTime
import java.util.UUID

data class Skjema(
    val tilstand: SkjemaTilstand,
    val meldekortId: Long,
    val ident: Ident,
    val meldeperiode: Periode,
    val payload: InnsendingPayload,
    val referanse: UUID?, //TODO - Fjern etter migrering
    val sendtInn: LocalDateTime?
)

enum class SkjemaTilstand {
    UTKAST,
    FORSØKER_Å_SENDE_TIL_ARENA,
    SENDT_ARENA,
    JOURNALFØRT,
}