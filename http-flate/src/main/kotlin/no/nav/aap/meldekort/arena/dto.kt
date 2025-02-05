package no.nav.aap.meldekort.arena

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.meldekort.Periode
import java.time.LocalDate

data class KommendeMeldekortDto(
    val antallUbesvarteMeldekort: Int,
    val nesteMeldekort: NesteMeldekortDto?
)

data class NesteMeldekortDto(
    val meldeperiode: PeriodeDto,
    val meldekortId: Long,
    val tidligsteInnsendingsDato: LocalDate,
    val kanSendesInn: Boolean,
)

data class HistoriskMeldekortDto(
    val meldeperiode: PeriodeDto,
    val status: MeldekortStatus,
)

data class HistoriskMeldekortDetaljerDto(
    val meldeperiode: PeriodeDto,
    val meldekortId: Long,
    val status: MeldekortStatus,
    val bruttoBeløp: Double?,
    val innsendtDato: LocalDate?,
    val kanEndres: Boolean,
    val timerArbeidet: List<TimerArbeidetDto>?,
    val type: MeldekortTypeDto,
) {
    constructor(historiskMeldekortDetaljer: ArenaSkjemaFlate.HistoriskMeldekortDetaljer) : this(
        meldeperiode = PeriodeDto(historiskMeldekortDetaljer.meldekort.periode),
        meldekortId = historiskMeldekortDetaljer.meldekort.meldekortId.asLong,
        status = historiskMeldekortDetaljer.meldekort.beregningStatus,
        bruttoBeløp = historiskMeldekortDetaljer.meldekort.bruttoBeløp,
        innsendtDato = historiskMeldekortDetaljer.meldekort.mottattIArena,
        kanEndres = historiskMeldekortDetaljer.meldekort.kanKorrigeres,
        timerArbeidet = historiskMeldekortDetaljer.timerArbeidet?.map(TimerArbeidetDto::fraDomene),
        type = MeldekortTypeDto.fraDomene(historiskMeldekortDetaljer.meldekort.type),
    )
}

data class MeldekortKorrigeringRequest(
    val timerArbeidet: List<TimerArbeidetDto>
)

enum class MeldekortTypeDto {
    VANLIG,
    ETTERREGISTRERING,
    KORRIGERING,
    UKJENT;

    companion object {
        fun fraDomene(type: MeldekortType): MeldekortTypeDto {
            return when (type) {
                MeldekortType.VANLIG -> VANLIG
                MeldekortType.ETTERREGISTRERING -> ETTERREGISTRERING
                MeldekortType.KORRIGERING -> KORRIGERING
                MeldekortType.UKJENT -> UKJENT
            }
        }
    }
}

class MeldekortSkjemaDto(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidetDto>,
    val stemmerOpplysningene: Boolean?
) {
    constructor(innsendingPayload: InnsendingPayload) : this(
        svarerDuSant = innsendingPayload.svarerDuSant,
        harDuJobbet = innsendingPayload.harDuJobbet,
        timerArbeidet = innsendingPayload.timerArbeidet.map { TimerArbeidetDto.fraDomene(it) },
        stemmerOpplysningene = innsendingPayload.stemmerOpplysningene,
    )

    fun tilDomene(): InnsendingPayload {
        return InnsendingPayload(
            svarerDuSant = svarerDuSant,
            harDuJobbet = harDuJobbet,
            timerArbeidet = timerArbeidet.map { it.tilDomene() },
            stemmerOpplysningene = stemmerOpplysningene
        )
    }
}

data class TimerArbeidetDto(
    val timer: Double?,
    val dato: LocalDate,
) {
    companion object {
        fun fraDomene(timerArbeidet: TimerArbeidet): TimerArbeidetDto {
            return TimerArbeidetDto(
                timer = timerArbeidet.timer,
                dato = timerArbeidet.dato
            )
        }
    }

    fun tilDomene(): TimerArbeidet {
        return TimerArbeidet(
            timer = timer,
            dato = dato
        )
    }
}

data class MeldekortRequest(
    val nåværendeSteg: StegNavn,
    val meldekort: MeldekortSkjemaDto
)

class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Periode) : this(periode.fom, periode.tom)
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
)
sealed interface Feil

class InnsendingFeil(
    val innsendingFeil: List<ArenaInnsendingFeiletException.InnsendingFeil>
) : Feil

data class MeldekortResponse(
    val steg: StegNavn,
    val periode: PeriodeDto,
    val meldekort: MeldekortSkjemaDto,
    val feil: Feil?
) {
    constructor(skjema: Utfylling, feil: Feil? = null) : this(
        steg = skjema.steg.navn,
        meldekort = MeldekortSkjemaDto(skjema.skjema.payload),
        periode = PeriodeDto(skjema.skjema.meldeperiode),
        feil = feil
    )
}
