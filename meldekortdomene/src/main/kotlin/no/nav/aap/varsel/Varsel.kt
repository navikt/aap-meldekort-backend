package no.nav.aap.varsel

import no.nav.aap.sak.Fagsaknummer
import java.time.Instant
import java.util.UUID

data class Varsel(
    val varselId: VarselId,
    val typeVarsel: TypeVarsel,
    val typeVarselTekst: TypeVarselTekst,
    val fagsaknummer: Fagsaknummer,
    val sendingstidspunkt: Instant,
    val status: VarselStatus
)

@JvmInline
value class VarselId(val id: UUID)

enum class TypeVarsel {
    BESKJED, OPPGAVE
}

enum class TypeVarselTekst {
    VALGFRITT_OPPLYSNINGSBEHOV, OPPLYSNINGSBEHOV, MELDEPLIKTPERIODE
}

enum class VarselStatus {
    PLANLAGT,
    SENDT,
    INAKTIVERT
}
