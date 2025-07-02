package no.nav.aap.varsel

import no.nav.aap.Periode
import no.nav.aap.sak.Fagsaknummer
import java.time.Instant
import java.util.UUID

data class Varsel(
    val varselId: VarselId,
    val typeVarsel: TypeVarsel,
    val typeVarselOm: TypeVarselOm,
    val saksnummer: Fagsaknummer,
    val sendingstidspunkt: Instant,
    val status: VarselStatus,
    val forPeriode: Periode,
    val opprettet: Instant,
    val sistEndret: Instant
)

@JvmInline
value class VarselId(val id: UUID) {
    override fun toString(): String {
        return id.toString()
    }
}

enum class TypeVarsel {
    BESKJED, OPPGAVE
}

enum class TypeVarselOm {
    VALGFRITT_OPPLYSNINGSBEHOV, OPPLYSNINGSBEHOV, MELDEPLIKTPERIODE
}

enum class VarselStatus {
    PLANLAGT,
    SENDT,
    INAKTIVERT
}
