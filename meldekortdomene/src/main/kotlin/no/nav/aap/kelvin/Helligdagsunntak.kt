package no.nav.aap.kelvin

import java.time.LocalDate

/**
 * Dokumentert på [Confluence](https://confluence.adeo.no/spaces/PAAP/pages/752102586/Tidlig+utbetaling+av+meldekort+i+forbindelse+med+jul+nytt%C3%A5r+og+p%C3%A5ske).
 */
private val helligdagsunntakJustertMeldedag: Map<LocalDate, LocalDate> = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
)

fun tidligsteInnsendingstidspunktMeldedag(startPåNesteMeldeperiode: LocalDate): LocalDate =
    (helligdagsunntakJustertMeldedag[startPåNesteMeldeperiode] ?: startPåNesteMeldeperiode)

fun originalInnsendingstidspunktMeldedag(tidligereInnsendingstidspunkt: LocalDate): LocalDate =
    helligdagsunntakJustertMeldedag.entries.firstOrNull { it.value == tidligereInnsendingstidspunkt }?.key
        ?: tidligereInnsendingstidspunkt

private val helligdagsunntakJustertMeldefrist: Map<LocalDate, LocalDate> = mapOf(
    LocalDate.of(2026, 4, 6) to LocalDate.of(2026, 4, 7),
    LocalDate.of(2026, 5, 25) to LocalDate.of(2026, 5, 26),
)

fun senesteInnsendingstidspunktMeldefrist(ordinærSisteDagMeldefrist: LocalDate): LocalDate =
    (helligdagsunntakJustertMeldefrist[ordinærSisteDagMeldefrist] ?: ordinærSisteDagMeldefrist)