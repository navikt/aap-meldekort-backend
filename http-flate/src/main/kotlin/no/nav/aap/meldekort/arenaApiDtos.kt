package no.nav.aap.meldekort

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.Periode
import java.time.LocalDate
import no.nav.aap.arena.ArenaInnsendingFeiletException
import no.nav.aap.arena.ArenaSkjemaFlate
import no.nav.aap.arena.InnsendingPayload
import no.nav.aap.arena.MeldekortStatus
import no.nav.aap.arena.MeldekortType
import no.nav.aap.arena.StegNavn
import no.nav.aap.arena.TimerArbeidet

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
    val type: MeldekortTypeDto,
    val harDuJobbet: Boolean,
    val harDuGjennomførtAvtaltAktivitetKursEllerUtdanning: Boolean,
    val harDuVærtSyk: Boolean,
    val harDuHattFerie: Boolean,
    val dager: List<DagerInfoDto>?,
) {
    constructor(historiskMeldekortDetaljer: ArenaSkjemaFlate.HistoriskMeldekortDetaljer) : this(
        meldeperiode = PeriodeDto(historiskMeldekortDetaljer.meldekort.periode),
        meldekortId = historiskMeldekortDetaljer.meldekort.meldekortId.asLong,
        status = historiskMeldekortDetaljer.meldekort.beregningStatus,
        bruttoBeløp = historiskMeldekortDetaljer.meldekort.bruttoBeløp,
        innsendtDato = historiskMeldekortDetaljer.meldekort.mottattIArena,
        kanEndres = historiskMeldekortDetaljer.meldekort.kanKorrigeres,
        dager = historiskMeldekortDetaljer.timerArbeidet?.map(DagerInfoDto.Companion::fraDomene),
        type = MeldekortTypeDto.fraDomene(historiskMeldekortDetaljer.meldekort.type),
        harDuJobbet = false,
        harDuGjennomførtAvtaltAktivitetKursEllerUtdanning = false,
        harDuVærtSyk = false,
        harDuHattFerie = false,
    )
}

data class MeldekortKorrigeringRequest(
    val harDuJobbet: Boolean,
    val harDuGjennomførtAvtaltAktivitetKursEllerUtdanning: Boolean,
    val harDuVærtSyk: Boolean,
    val harDuHattFerie: Boolean,
    val dager: List<DagerInfoDto>
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
    val harDuGjennomførtAvtaltAktivitetKursEllerUtdanning: Boolean?,
    val harDuVærtSyk: Boolean?,
    val harDuHattFerie: Boolean?,
    val dager: List<DagerInfoDto>,
    val stemmerOpplysningene: Boolean?
) {
    constructor(innsendingPayload: InnsendingPayload) : this(
        svarerDuSant = innsendingPayload.svarerDuSant,
        harDuJobbet = innsendingPayload.harDuJobbet,
        harDuGjennomførtAvtaltAktivitetKursEllerUtdanning = null,
        harDuVærtSyk = null,
        harDuHattFerie = null,
        dager = innsendingPayload.timerArbeidet.map { DagerInfoDto.fraDomene(it) },
        stemmerOpplysningene = innsendingPayload.stemmerOpplysningene,
    )

    fun tilDomene(): InnsendingPayload {
        return InnsendingPayload(
            svarerDuSant = svarerDuSant,
            harDuJobbet = harDuJobbet,
            timerArbeidet = dager.map { it.tilDomene() },
            stemmerOpplysningene = stemmerOpplysningene
        )
    }
}

data class DagerInfoDto(
    val dato: LocalDate,
    val timerArbeidet: Double?,
    val harVærtPåtiltakKursEllerUtdanning: Boolean? = false,
    val harVærtPåFerie: Boolean? = false,
    val harVærtSyk: Boolean? = false,
) {
    companion object {
        fun fraDomene(timerArbeidet: TimerArbeidet): DagerInfoDto {
            return DagerInfoDto(
                timerArbeidet = timerArbeidet.timer,
                dato = timerArbeidet.dato
            )
        }
    }

    fun tilDomene(): TimerArbeidet {
        return TimerArbeidet(
            timer = timerArbeidet,
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
    val feil: Feil?,
    val tidligsteInnsendingsDato: LocalDate,
) {
    constructor(utfyllingResponse: ArenaSkjemaFlate.UtfyllingResponse) : this(
        steg = utfyllingResponse.utfylling.steg.navn,
        meldekort = MeldekortSkjemaDto(utfyllingResponse.utfylling.skjema.payload),
        periode = PeriodeDto(utfyllingResponse.utfylling.skjema.meldeperiode),
        tidligsteInnsendingsDato = utfyllingResponse.meldekort.tidligsteInnsendingsdato,
        feil = utfyllingResponse.feil?.let { InnsendingFeil(it.innsendingFeil) },
    )
}
