package no.nav.aap.meldekort

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class StartUtfyllingRequest(
    val fom: LocalDate,
    val tom: LocalDate,
)

class StartUtfyllingResponse(
    val metadata: UtfyllingMetadataDto?,
    val tilstand: UtfyllingTilstandDto?,
    val feil: String?,
)

class EndreUtfyllingRequest @JsonCreator constructor(
    val nyTilstand: UtfyllingTilstandDto,
)

class UtfyllingResponseDto(
    val metadata: UtfyllingMetadataDto,
    val tilstand: UtfyllingTilstandDto,
    val feil: String?
)

class UtfyllingTilstandDto(
    val aktivtSteg: StegDto,
    val svar: SvarDto,
) {
    constructor(utfylling: Utfylling) : this(
        aktivtSteg = StegDto.fraDomene(utfylling.aktivtSteg),
        svar = SvarDto(utfylling.svar)
    )
}

class UtfyllingMetadataDto(
    val referanse: UUID,
    val periode: PeriodeDto,
    val antallUbesvarteMeldeperioder: Int,

    /** Hvis `null` skal det tolkes som at det kan sendes inn når som helst.
     * Foreløpig er den alltid satt, siden logikken ikke er implementert skikkelig
     * enda.
     */
    val tidligsteInnsendingstidspunkt: LocalDateTime?,

    /** Hvis `null` skal det tolkes som at det ikke er en bestemt frist som i seg selv
     * påvirker ytelsen.
     * Foreløpig er den alltid satt, siden logikken ikke er implementert skikkelig
     * enda.
     */
    val fristForInnsending: LocalDateTime?,
    val kanSendesInn: Boolean,
) {
    companion object {
        fun fraDomene(utfylling: Utfylling, antallUbesvarteMeldeperioder: Int): UtfyllingMetadataDto {
            /* TODO: denne logikken burde flyttes ut av dto-koden. */
            val tidligsteInnsendingstidspunkt = utfylling.periode.tom.plusDays(1).atStartOfDay()
            val fristForInnsending = utfylling.periode.tom.plusDays(9).atTime(23, 59)
            val kanSendesInn = tidligsteInnsendingstidspunkt <= LocalDateTime.now(ZoneId.of("Europe/Oslo"))

            return UtfyllingMetadataDto(
                referanse = utfylling.referanse.asUuid,
                periode = PeriodeDto(utfylling.periode),
                tidligsteInnsendingstidspunkt = tidligsteInnsendingstidspunkt,
                fristForInnsending = fristForInnsending,
                kanSendesInn = kanSendesInn,
                antallUbesvarteMeldeperioder = antallUbesvarteMeldeperioder,
            )
        }
    }
}

enum class StegDto(val tilDomene: UtfyllingStegNavn) {
    INTRODUKSJON(UtfyllingStegNavn.INTRODUKSJON),
    SPØRSMÅL(UtfyllingStegNavn.SPØRSMÅL),
    UTFYLLING(UtfyllingStegNavn.UTFYLLING),
    BEKREFT(UtfyllingStegNavn.BEKREFT),
    KVITTERING(UtfyllingStegNavn.KVITTERING),
    ;

    companion object {
        fun fraDomene(navn: UtfyllingStegNavn): StegDto {
            return when (navn) {
                UtfyllingStegNavn.INTRODUKSJON -> INTRODUKSJON
                UtfyllingStegNavn.SPØRSMÅL -> SPØRSMÅL
                UtfyllingStegNavn.UTFYLLING -> UTFYLLING
                UtfyllingStegNavn.BEKREFT -> BEKREFT
                UtfyllingStegNavn.KVITTERING -> KVITTERING

                UtfyllingStegNavn.ARENAKONTROLL_VANLIG,
                UtfyllingStegNavn.ARENAKONTROLL_KORRIGERING,
                UtfyllingStegNavn.PERSISTER_OPPLYSNINGER,
                UtfyllingStegNavn.BESTILL_JOURNALFØRING -> error("skal ikke stoppe i teknisk steg")
            }
        }
    }
}

class SvarDto(
    val vilSvareRiktig: Boolean?,
    val harDuJobbet: Boolean?,
    val dager: List<DagSvarDto>,
    val stemmerOpplysningene: Boolean?,
) {
    fun tilDomene(): Svar {
        return Svar(
            svarerDuSant = vilSvareRiktig,
            harDuJobbet = harDuJobbet,
            timerArbeidet = dager.map { it.tilTimerArbeidet() },
            stemmerOpplysningene = stemmerOpplysningene,
        )
    }

    constructor(svar: Svar) : this(
        vilSvareRiktig = svar.svarerDuSant,
        harDuJobbet = svar.harDuJobbet,
        dager = svar.timerArbeidet.map { DagSvarDto(it) },
        stemmerOpplysningene = svar.stemmerOpplysningene,
    )
}

class DagSvarDto(
    val dato: LocalDate,
    val timerArbeidet: Double?,
) {
    fun tilTimerArbeidet(): TimerArbeidet {
        return TimerArbeidet(
            timer = timerArbeidet,
            dato = dato,
        )
    }

    constructor(timerArbeidet: TimerArbeidet) : this(
        dato = timerArbeidet.dato,
        timerArbeidet = timerArbeidet.timer,
    )
}

