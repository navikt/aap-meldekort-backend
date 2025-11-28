package no.nav.aap.kelvin

import java.time.LocalDate

/**
 * Dokumentert på [Confluence](https://confluence.adeo.no/spaces/PAAP/pages/752102586/Tidlig+utbetaling+av+meldekort+i+forbindelse+med+jul+nytt%C3%A5r+og+p%C3%A5ske).
 */
private val helligdagsunntak: Map<LocalDate, LocalDate> = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
    LocalDate.of(2026, 3, 30) to LocalDate.of(2026, 3, 25)
)

fun tidligsteInnsendingstidspunkt(startPåNesteMeldeperiode: LocalDate): LocalDate =
    (helligdagsunntak[startPåNesteMeldeperiode] ?: startPåNesteMeldeperiode)

fun originalInnsendingstidspunkt(tidligereInnsendingstidspunkt: LocalDate): LocalDate =
    helligdagsunntak.entries.firstOrNull { it.value == tidligereInnsendingstidspunkt }?.key
        ?: tidligereInnsendingstidspunkt