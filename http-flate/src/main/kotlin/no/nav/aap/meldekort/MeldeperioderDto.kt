package no.nav.aap.meldekort

import java.time.LocalDate

class MeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val innsendingsvindu: PeriodeDto,
)

class KommendeMeldeperioderDto(
    val antallUbesvarteMeldeperioder: Int,
    val nesteMeldeperiode: MeldeperiodeDto?,
)

class HistoriskMeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val status: MeldeperiodeStatusDto,
)

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
}
