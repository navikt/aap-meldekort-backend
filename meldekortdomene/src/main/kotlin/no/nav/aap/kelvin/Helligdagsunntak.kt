package no.nav.aap.kelvin

import no.nav.aap.komponenter.miljo.Miljø
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate

/**
 * Dokumentert på [Confluence](https://confluence.adeo.no/spaces/PAAP/pages/752102586/Tidlig+utbetaling+av+meldekort+i+forbindelse+med+jul+nytt%C3%A5r+og+p%C3%A5ske).
 */
val EKSISTERENDE_MELDEDAGUNNTAK_I_PROD = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
)

val TEST_MELDEDAGUNNTAK_I_DEV = mapOf(
    LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
    LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2)
)

val KOMMENDE_MELDEDAGUNNTAK_I_PROD = mapOf(
    LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
)

@VisibleForTesting
internal fun helligdagsunntakJustertMeldedag(erProd: Boolean = Miljø.erProd()): Map<LocalDate, LocalDate> =
    EKSISTERENDE_MELDEDAGUNNTAK_I_PROD + if (!erProd) (TEST_MELDEDAGUNNTAK_I_DEV + KOMMENDE_MELDEDAGUNNTAK_I_PROD) else emptyMap()

private val erProd = Miljø.erProd()

val aktiveHelligdagsunntakForJustertMeldedag = helligdagsunntakJustertMeldedag(erProd)

fun tidligsteInnsendingstidspunktMeldedag(startPåNesteMeldeperiode: LocalDate): LocalDate =
    (aktiveHelligdagsunntakForJustertMeldedag[startPåNesteMeldeperiode] ?: startPåNesteMeldeperiode)

fun originalInnsendingstidspunktMeldedag(tidligereInnsendingstidspunkt: LocalDate): LocalDate =
    helligdagsunntakJustertMeldedag().entries.firstOrNull { it.value == tidligereInnsendingstidspunkt }?.key
        ?: tidligereInnsendingstidspunkt

private val helligdagsunntakJustertMeldefrist: Map<LocalDate, LocalDate> = mapOf(
    LocalDate.of(2026, 4, 6) to LocalDate.of(2026, 4, 7),
    LocalDate.of(2026, 5, 25) to LocalDate.of(2026, 5, 26),
)

fun senesteInnsendingstidspunktMeldefrist(ordinærSisteDagMeldefrist: LocalDate): LocalDate =
    (helligdagsunntakJustertMeldefrist[ordinærSisteDagMeldefrist] ?: ordinærSisteDagMeldefrist)