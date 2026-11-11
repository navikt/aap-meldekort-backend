package no.nav.aap.kelvin

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class HelligdagsunntakTest {

    /** Sikkerhetsnett for å forsøke å redusere single point of failure for tilbakekreving
     * ved endringer på unntaksdatoer i Helligdagsunntak.kt
     *
     * Legg til ny test for hver nye unntaksdato-tupel som legges til i Helligdagsunntak.kt
     */
    @Test
    fun `prodmiljø har meldedagsunntak for 2025-12-22`() {
        val prodUnntak = helligdagsunntakJustertMeldedag(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 17))
    }

    @Test
    fun `testmiljø har forventet unntaksutvalg for meldedager`() {
        val testUnntak = helligdagsunntakJustertMeldedag(erProd = false)

        val forventetUtvalg = mapOf(
            LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
            LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
            LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2),
            LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
        )

        assertThat(testUnntak).isEqualTo(forventetUtvalg)
    }

}
