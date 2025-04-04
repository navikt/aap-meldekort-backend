package no.nav.aap.meldekort

import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import java.time.LocalDate

class MeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val innsendingsvindu: PeriodeDto,
) {
    constructor(meldeperiode: Meldeperiode): this(
        meldeperiode = PeriodeDto(meldeperiode.meldeperioden),
        innsendingsvindu = PeriodeDto(meldeperiode.meldevindu),
    )

    companion object {
        fun ellerNull(domene: Meldeperiode?): MeldeperiodeDto? {
            if (domene == null) {
                return null
            }

            return MeldeperiodeDto(
                meldeperiode = PeriodeDto(domene.meldeperioden),
                innsendingsvindu = PeriodeDto(domene.meldevindu),
            )
        }
    }
}

class KommendeMeldeperioderDto(
    val antallUbesvarteMeldeperioder: Int,
    val manglerOpplysninger: PeriodeDto?,
    val nesteMeldeperiode: MeldeperiodeDto?,
) {
    companion object {
        fun fraDomene(domene: MeldeperiodeFlate.KommendeMeldeperioder): KommendeMeldeperioderDto {
            return KommendeMeldeperioderDto(
                antallUbesvarteMeldeperioder = domene.antallUbesvarteMeldeperioder,
                manglerOpplysninger = PeriodeDto.ellerNull(domene.manglerOpplysninger),
                nesteMeldeperiode = MeldeperiodeDto.ellerNull(domene.nesteMeldeperiode),
            )

        }
    }
}

class HistoriskMeldeperiodeDto(
    val antallTimerArbeidetIPerioden: Double,
    val meldeperiode: PeriodeDto,
    val status: MeldeperiodeStatusDto,
) {
    companion object {
        fun fraDomene(domene: MeldeperiodeFlate.HistoriskMeldeperiode): HistoriskMeldeperiodeDto {
            return HistoriskMeldeperiodeDto(
                antallTimerArbeidetIPerioden = domene.totaltAntallTimerIPerioden,
                meldeperiode = PeriodeDto(domene.meldeperiode.meldeperioden),
                status = MeldeperiodeStatusDto.KELVIN, /* TODO: arena */
            )
        }
    }
}

enum class MeldeperiodeStatusDto {
    ARENA_INNSENDT,
    ARENA_FERDIG,
    ARENA_FEILET,

    KELVIN,
}

enum class MeldekortTypeDto {
    ARENA_VANLIG,
    ARENA_ETTERREGISTRERING,
    ARENA_KORRIGERING,
    ARENA_ANNET,

    KELVIN,
}

data class PeriodeDetaljerDto(
    val periode: PeriodeDto,
    val status: MeldeperiodeStatusDto,
    val bruttoBeløp: Double?,
    val innsendtDato: LocalDate?,
    val kanEndres: Boolean,
    val type: MeldekortTypeDto,
    val svar: SvarDto,
) {
    constructor(detaljer: MeldeperiodeFlate.PeriodeDetaljer) : this(
        periode = PeriodeDto(detaljer.periode),
        status = MeldeperiodeStatusDto.KELVIN /* TODO: arena */,
        bruttoBeløp = null /* TODO: arena */,
        innsendtDato = null /* TODO */,
        kanEndres = true /* TODO: arena */,
        type = MeldekortTypeDto.KELVIN /* TODO: arena */,
        svar = SvarDto(detaljer.svar)
    )
}
