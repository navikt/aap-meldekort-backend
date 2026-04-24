package no.nav.aap.meldekort

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.FellesLandingssideService
import no.nav.aap.arena.MeldekortServiceGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FellesLandingssideServiceTest {

    private val fnr = "12345678901"
    private val gateway = mockk<MeldekortServiceGateway>()
    private val service = FellesLandingssideService(gateway)

    private fun arenaMeldekort(
        meldekortId: Long = 1L,
        hoyesteMeldegruppe: String = "AAP",
        tilDato: LocalDate = LocalDate.of(2026, 4, 20),
        mottattDato: LocalDate? = null,
    ) = ArenaMeldekort(
        meldekortId = meldekortId,
        kortType = "ELEKTRONISK",
        meldeperiode = "202616",
        fraDato = tilDato.minusDays(13),
        tilDato = tilDato,
        hoyesteMeldegruppe = hoyesteMeldegruppe,
        beregningstatus = "OPPRE",
        forskudd = false,
        mottattDato = mottattDato,
    )

    @Test
    fun `returnerer null når Arena ikke kjenner brukeren`() {
        every { gateway.hentMeldekort(fnr) } returns null

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNull()
    }

    @Test
    fun `returnerer null når Arena ikke har meldekort og ingen historiske meldekort finnes`() {
        every { gateway.hentMeldekort(fnr) } returns emptyList()
        every { gateway.hentHistoriskeMeldekort(fnr) } returns emptyList()

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNull()
    }

    @Test
    fun `returnerer meldekortstatus med meldekort til utfylling når Arena har AAP-meldekort som ikke er sendt inn`() {
        val tilDato = LocalDate.of(2026, 4, 20)
        every { gateway.hentMeldekort(fnr) } returns listOf(arenaMeldekort(tilDato = tilDato, mottattDato = null))
        every { gateway.hentHistoriskeMeldekort(fnr) } returns emptyList()

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isFalse()
        assertThat(resultat.meldekortTilUtfylling).hasSize(1)
        assertThat(resultat.meldekortTilUtfylling[0].kanSendesFra).isEqualTo(tilDato.minusDays(1))
    }

    @Test
    fun `harInnsendteMeldekort er true når Arena har AAP-meldekort med mottattDato`() {
        every { gateway.hentMeldekort(fnr) } returns listOf(arenaMeldekort(mottattDato = LocalDate.of(2026, 4, 15)))

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isTrue()
    }

    @Test
    fun `ikke-AAP meldekort fra Arena ignoreres - returnerer null`() {
        every { gateway.hentMeldekort(fnr) } returns listOf(arenaMeldekort(hoyesteMeldegruppe = "ATTF", mottattDato = null))
        every { gateway.hentHistoriskeMeldekort(fnr) } returns emptyList()

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNull()
    }

    @Test
    fun `harInnsendteMeldekort er true når historiske Arena-meldekort er sendt inn selv om ingen aktive AAP-meldekort finnes`() {
        every { gateway.hentMeldekort(fnr) } returns emptyList()
        every { gateway.hentHistoriskeMeldekort(fnr) } returns listOf(arenaMeldekort(mottattDato = LocalDate.of(2026, 3, 1)))

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isTrue()
        assertThat(resultat.meldekortTilUtfylling).isEmpty()
    }

    @Test
    fun `meldekortTilUtfylling inneholder kun AAP-meldekort når Arena returnerer blanding`() {
        val aapTilDato = LocalDate.of(2026, 4, 20)
        every { gateway.hentMeldekort(fnr) } returns listOf(
            arenaMeldekort(meldekortId = 1L, hoyesteMeldegruppe = "AAP", tilDato = aapTilDato),
            arenaMeldekort(meldekortId = 2L, hoyesteMeldegruppe = "ATTF", tilDato = aapTilDato),
        )
        every { gateway.hentHistoriskeMeldekort(fnr) } returns emptyList()

        val resultat = service.hentFraArena(fnr)

        assertThat(resultat).isNotNull
        assertThat(resultat!!.meldekortTilUtfylling).hasSize(1)
    }
}
