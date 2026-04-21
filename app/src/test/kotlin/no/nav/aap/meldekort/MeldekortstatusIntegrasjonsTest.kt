package no.nav.aap.meldekort

import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.meldekort.test.FakeArena
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortstatusIntegrasjonsTest {

    @AutoClose
    val app = AppInstance(6 januar 2025)

    @AfterEach
    fun tearDown() {
        FakeArena.clear()
    }

    private fun arenaMeldekort(
        tilDato: LocalDate,
        mottattDato: LocalDate? = null,
    ) = ArenaMeldekort(
        meldekortId = 1L,
        kortType = "ELEKTRONISK",
        meldeperiode = "202608",
        fraDato = tilDato.minusDays(13),
        tilDato = tilDato,
        hoyesteMeldegruppe = "AAP",
        beregningstatus = "OPPRE",
        forskudd = false,
        mottattDato = mottattDato,
    )

    @Test
    fun `bruker uten meldekort i Kelvin får meldekort fra Arena`() {
        val fnr = fødselsnummerGenerator.next()
        val tilDato = LocalDate.of(2025, 1, 19)

        app.arenaMeldekort(fnr, listOf(arenaMeldekort(tilDato = tilDato)))

        val status = app.get<MeldekortstatusDto>(fnr, "/api/meldekort-status")

        assertThat(status).isNotNull
        assertThat(status!!.meldekortTilUtfylling).hasSize(1)
        assertThat(status.meldekortTilUtfylling[0].kanSendesFra).isEqualTo(tilDato.minusDays(1))
        assertThat(status.harInnsendteMeldekort).isFalse()
    }
}
