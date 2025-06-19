package no.nav.aap.varsel

import no.nav.aap.sak.Fagsaknummer
import java.time.Instant
import java.util.UUID

data class Varsel(
    val varselId: VarselId,
    val varselType: VarselType,
    val fagsaknummer: Fagsaknummer,
    val sendingstidspunkt: Instant,
    val status: VarselStatus
)

@JvmInline
value class VarselId(val id: UUID)

enum class VarselType {
    VARSEL, OPPGAVE
}

enum class VarselStatus {
    PLANLAGT,
    SENDT,
    INAKTIVERT
}
