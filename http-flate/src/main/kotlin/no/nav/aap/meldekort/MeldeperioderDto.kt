package no.nav.aap.meldekort

import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.FagsystemService
import java.time.LocalDate

class MeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val innsendingsvindu: PeriodeDto,
) {
    constructor(meldeperiode: Meldeperiode): this(
        meldeperiode = PeriodeDto(meldeperiode.meldeperioden),
        innsendingsvindu = PeriodeDto(meldeperiode.meldevindu),
    )
}

class KommendeMeldeperioderDto(
    val antallUbesvarteMeldeperioder: Int,
    val nesteMeldeperiode: MeldeperiodeDto?,
)

class HistoriskMeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val status: MeldeperiodeStatusDto,
) {
    constructor(meldeperiode: Meldeperiode) : this(
        meldeperiode = PeriodeDto(meldeperiode.meldeperioden),
        status = MeldeperiodeStatusDto.KELVIN /* TODO */
    )
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
    constructor(detaljer: FagsystemService.PeriodeDetaljer) : this(
        periode = PeriodeDto(detaljer.periode),
        status = MeldeperiodeStatusDto.KELVIN /* TODO */,
        bruttoBeløp = null /* TODO */,
        innsendtDato = null /* TODO */,
        kanEndres = false /* TODO */,
        type = MeldekortTypeDto.KELVIN /* TODO */,
        svar = SvarDto(detaljer.svar)
    )
}
