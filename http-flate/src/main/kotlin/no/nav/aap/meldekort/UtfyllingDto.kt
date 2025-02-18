package no.nav.aap.meldekort

import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.LocalDate
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

class HentUtfyllingResponse(
    val metadata: UtfyllingMetadataDto,
    val tilstand: UtfyllingTilstandDto,
)

class EndreUtfyllingRequest(
    val nyTilstand: UtfyllingTilstandDto,
)

class EndreUtfyllingResponse(
    val utfyllingTilstand: UtfyllingTilstandDto,
    val feil: String?
)

class UtfyllingTilstandDto(
    val steg: StegDto,
    val svar: SvarDto,
) {
    constructor(utfylling: Utfylling) : this(
        steg = StegDto.fraDomene(utfylling.aktivtSteg.navn),
        svar = SvarDto(utfylling.svar)
    )
}

class UtfyllingMetadataDto(
    val referanse: UUID,
    val periode: PeriodeDto,
) {
    constructor(utfylling: Utfylling): this(
        referanse = utfylling.referanse.asUuid,
        periode = PeriodeDto(utfylling.periode),
    )
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

    constructor(svar: Svar): this(
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

    constructor(timerArbeidet: TimerArbeidet): this(
        dato = timerArbeidet.dato,
        timerArbeidet = timerArbeidet.timer,
    )
}

