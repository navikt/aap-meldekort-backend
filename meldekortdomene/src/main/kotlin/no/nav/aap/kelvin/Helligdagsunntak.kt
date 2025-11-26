package no.nav.aap.kelvin

import java.time.LocalDate

private val helligdagsunntak: Map<LocalDate, LocalDate> = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
)

fun tidligsteInnsendingstidspunkt(startPåNesteMeldeperiode: LocalDate): LocalDate =
    (helligdagsunntak[startPåNesteMeldeperiode] ?: startPåNesteMeldeperiode)