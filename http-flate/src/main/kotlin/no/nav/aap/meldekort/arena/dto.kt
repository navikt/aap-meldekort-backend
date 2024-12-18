package no.nav.aap.meldekort.arena

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.meldekort.Periode
import java.time.LocalDate


@Suppress("unused")
class MeldeperiodeDto(
    val meldekortId: Long,
    val periode: PeriodeDto,
    val type: MeldeperiodeTypeDto,
    val klarForInnsending: Boolean,
    val kanEndres: Boolean,
) {
    constructor(meldekort: Meldekort) : this(
        meldekortId = meldekort.meldekortId,
        periode = PeriodeDto(meldekort.periode),
        type = MeldeperiodeTypeDto.fraDomene(meldekort.type),
        klarForInnsending = meldekort is KommendeMeldekort && meldekort.kanSendes,
        kanEndres = meldekort.kanKorrigeres,
    )

    enum class MeldeperiodeTypeDto {
        VANLIG,
        ETTERREGISTRERING,
        KORRIGERING,
        UKJENT;

        companion object {
            fun fraDomene(type: MeldekortType): MeldeperiodeTypeDto {
                return when (type) {
                    MeldekortType.VANLIG -> VANLIG
                    MeldekortType.ETTERREGISTRERING -> ETTERREGISTRERING
                    MeldekortType.KORRIGERING -> KORRIGERING
                    MeldekortType.UKJENT -> UKJENT
                }
            }
        }
    }
}


@Suppress("unused")
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
    constructor(skjema: Skjema, feil: Feil? = null) : this(
        steg = skjema.steg.navn,
        meldekort = MeldekortSkjemaDto(skjema.payload),
        periode = PeriodeDto(skjema.meldeperiode),
        feil = feil
    )
}
