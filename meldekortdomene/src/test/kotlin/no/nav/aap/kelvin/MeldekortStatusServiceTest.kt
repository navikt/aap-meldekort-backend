package no.nav.aap.kelvin

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.Fagsaknummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MeldekortStatusServiceTest {

    private val ident = Ident("12345678901")

    private val andreMars = LocalDate.of(2026, 3, 2)
    private val clock = Clock.fixed(
        Instant.from(andreMars.atStartOfDay(ZoneId.of("UTC")).toInstant()),
        ZoneId.of("UTC")
    )

    private val kelvinSakRepository = mockk<KelvinSakRepository>()
    private val aktivitetsInformasjonRepository = mockk<AktivitetsInformasjonRepository>()
    private val kelvinSakService = mockk<KelvinSakService>()

    private val service = MeldekortStatusService(
        kelvinSakRepository = kelvinSakRepository,
        aktivitetsInformasjonRepository = aktivitetsInformasjonRepository,
        kelvinSakService = kelvinSakService,
        clock = clock,
    )

    private val sak = KelvinSak(
        saksnummer = Fagsaknummer("1234"),
        status = KelvinSakStatus.LØPENDE,
        rettighetsperiode = Periode(andreMars.minusMonths(1), andreMars.plusMonths(11)),
    )

    @Test
    fun `brukerHarSakIKelvin returnerer sak når bruker har aktiv sak`() {
        every { kelvinSakRepository.hentSak(ident, andreMars) } returns sak

        val result = service.brukerHarSakIKelvin(ident)

        assertThat(result).isEqualTo(sak)
    }

    @Test
    fun `brukerHarSakIKelvin returnerer null når bruker ikke har sak`() {
        every { kelvinSakRepository.hentSak(ident, andreMars) } returns null

        val result = service.brukerHarSakIKelvin(ident)

        assertThat(result).isNull()
    }

    @Test
    fun `hentMeldekortTilUtfylling returnerer tom liste når bruker ikke har noen meldekort å sende inn`() {
        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns emptyList()

        val result = service.hentMeldekortTilUtfylling(ident, sak.referanse)

        assertThat(result).isEmpty()
    }

    @Test
    fun `hentMeldekortTilUtfylling henter meldekort som enda ikke er utfylt`() {
        val toUkerSiden = andreMars.minusDays(14)
        val dagensdato = andreMars

        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns listOf(
            meldeperiode(meldevinduFom = toUkerSiden, meldevinduTom = toUkerSiden.plusDays(7)),
            meldeperiode(meldevinduFom = dagensdato, meldevinduTom = dagensdato.plusDays(7)),
        )
        val result = service.hentMeldekortTilUtfylling(ident, sak.referanse)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.kanSendesFra }).containsExactly(toUkerSiden, dagensdato)
    }

    @Test
    fun `hentMeldekortTilUtfylling ekskluderer meldeperioder der meldevindu fom er i fremtiden`() {
        val kanSendesInnFra = andreMars.plusDays(1)
        val toUkerSiden = andreMars.minusDays(14)

        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns listOf(
            meldeperiode(meldevinduFom = toUkerSiden, meldevinduTom = toUkerSiden.plusDays(7)),
            meldeperiode(meldevinduFom = kanSendesInnFra, meldevinduTom = kanSendesInnFra.plusDays(7)),
        )

        val result = service.hentMeldekortTilUtfylling(ident, sak.referanse)

        assertThat(result).hasSize(1)
        assertThat(result.map { it.kanSendesFra }).containsExactly(toUkerSiden)
    }
    // --- ok til hit ^^^

    private fun meldeperiode(
        meldeperiodeFom: LocalDate = andreMars.minusDays(14),
        meldeperiodeTom: LocalDate = andreMars.minusDays(1),
        meldevinduFom: LocalDate = andreMars,
        meldevinduTom: LocalDate = andreMars.plusDays(7),
    ) = Meldeperiode(
        meldeperioden = Periode(meldeperiodeFom, meldeperiodeTom),
        meldevindu = Periode(meldevinduFom, meldevinduTom),
    )
}
