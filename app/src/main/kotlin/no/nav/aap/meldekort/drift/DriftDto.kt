package no.nav.aap.meldekort.drift

import no.nav.aap.Periode
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingStegNavn
import no.nav.aap.varsel.Varsel
import java.time.Instant
import java.util.UUID

internal data class MeldekortDriftsinfoDto(
    val aktuelleMeldeperioder: List<AktuelleMeldeperioderDriftsinfo>,
    val historiskeMeldeperioder: List<HistoriskeMeldeperioderDriftsinfo>,
    val utfyllinger: List<UtfyllingDriftsinfo>,
    val varsler: List<VarselDriftsinfo>,
)

internal data class AktuelleMeldeperioderDriftsinfo(
    val antallUbesvarteMeldeperioder: Int,
    val manglerOpplysninger: Periode?,
    val nesteMeldeperiode: Meldeperiode?,
) {
    companion object {
        fun fra(meldeperioder: MeldeperiodeFlate.KommendeMeldeperioder) = AktuelleMeldeperioderDriftsinfo(
            meldeperioder.antallUbesvarteMeldeperioder,
            meldeperioder.manglerOpplysninger,
            meldeperioder.nesteMeldeperiode
        )
    }
}

internal data class HistoriskeMeldeperioderDriftsinfo(
    val meldeperiode: Meldeperiode,
    val totaltAntallTimerIPerioden: Double,
) {
    companion object {
        fun fra(meldeperiode: MeldeperiodeFlate.HistoriskMeldeperiode) = HistoriskeMeldeperioderDriftsinfo(
            meldeperiode.meldeperiode,
            meldeperiode.totaltAntallTimerIPerioden,
        )
    }
}

internal data class UtfyllingDriftsinfo(
    val referanse: UUID,
    val fagsak: FagsakReferanse,
    val periode: Periode,
    val flyt: UtfyllingFlytNavn,
    val aktivtSteg: UtfyllingStegNavn,
    val svar: Svar,
    val opprettet: Instant,
    val sistEndret: Instant,
    val erDigitalisert: Boolean?
) {
    companion object {
        fun fra(utfylling: Utfylling) = UtfyllingDriftsinfo(
            utfylling.referanse.asUuid,
            utfylling.fagsak,
            utfylling.periode,
            utfylling.flyt,
            utfylling.aktivtSteg,
            utfylling.svar,
            utfylling.opprettet,
            utfylling.sistEndret,
            utfylling.erDigitalisert
        )
    }
}

internal data class VarselDriftsinfo(
    val varselId: UUID,
    val typeVarsel: String,
    val typeVarselOm: String,
    val saksnummer: String,
    val sendingstidspunkt: Instant,
    val status: String,
    val forPeriode: Periode,
    val opprettet: Instant,
    val sistEndret: Instant
) {
    companion object {
        fun fra(varsel: Varsel) = VarselDriftsinfo(
            varsel.varselId.id,
            varsel.typeVarsel.name,
            varsel.typeVarselOm.name,
            varsel.saksnummer.asString,
            varsel.sendingstidspunkt,
            varsel.status.name,
            varsel.forPeriode,
            varsel.opprettet,
            varsel.sistEndret
        )
    }
}