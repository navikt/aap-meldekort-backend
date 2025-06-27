package no.nav.aap.meldekort.varsel

import no.nav.aap.Periode
import no.nav.aap.meldekort.saksnummerGenerator
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.varsel.TypeVarsel
import no.nav.aap.varsel.TypeVarselOm
import no.nav.aap.varsel.Varsel
import no.nav.aap.varsel.VarselId
import no.nav.aap.varsel.VarselStatus
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

fun byggVarsel(
    saksnummer: Fagsaknummer = saksnummerGenerator.next(),
    varselId: VarselId = VarselId(UUID.randomUUID()),
    typeVarsel: TypeVarsel = TypeVarsel.OPPGAVE,
    typeVarselOm: TypeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
    sendingstidspunkt: Instant = Instant.now(),
    status: VarselStatus = VarselStatus.PLANLAGT,
    forPeriode: Periode = Periode(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 22)),
    opprettet: Instant = Instant.now(),
    sistEndret: Instant = Instant.now()
): Varsel {
    return Varsel(
        varselId = varselId,
        typeVarsel = typeVarsel,
        typeVarselOm = typeVarselOm,
        saksnummer = saksnummer,
        sendingstidspunkt = sendingstidspunkt.truncatedTo(ChronoUnit.MILLIS),
        status = status,
        forPeriode = forPeriode,
        opprettet = opprettet.truncatedTo(ChronoUnit.MILLIS),
        sistEndret = sistEndret.truncatedTo(ChronoUnit.MILLIS)
    )
}
