package no.nav.aap.kelvin

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.opplysningsplikt.AktivitetsInformasjon
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.utfylling.UtfyllingReferanse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.util.*

class KelvinMeldeperiodeFlateTest {

    val innloggetBruker = InnloggetBruker(Ident("12345678901"), "test-token")

    val aktivitetsInformasjonRepository = mockk<AktivitetsInformasjonRepository>()
    val kelvinSakRepository = mockk<KelvinSakRepository>()
    val clock = Clock.systemDefaultZone()

    val sakService = KelvinSakService(
        kelvinSakRepository = kelvinSakRepository,
        aktivitetsInformasjonRepository = aktivitetsInformasjonRepository,
        clock = clock
    )
    val meldeperiodeFlate = KelvinMeldeperiodeFlate(
        sakService = sakService,
        kelvinSakRepository = kelvinSakRepository,
        aktivitetsInformasjonRepository = aktivitetsInformasjonRepository,
        clock = clock,
    )

    val totalPeriode = Periode(LocalDate.now(clock).minusWeeks(8), LocalDate.now(clock).plusWeeks(44))
    val sak = KelvinSak(
        saksnummer = Fagsaknummer("1234"),
        status = KelvinSakStatus.UTREDES,
        rettighetsperiode = totalPeriode,
    )

    val meldeperioder = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom).slidingWindow(
        size = 14,
        step = 14,
        partialWindows = true
    )

    val førstePeriode = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusDays(13))

    @BeforeEach
    fun setUp() {
        every { kelvinSakRepository.hentSak(any(), any()) } returns sak
        every { kelvinSakRepository.hentMeldeperioder(any()) } returns meldeperioder
        every { kelvinSakRepository.hentOpplysningsbehov(any()) } returns meldeperioder
    }

    @Test
    fun `skal hente ut aktuelle meldekort hvor ingen er fylt ut med meldekort 8 uker tilbake i tid`() {
        every { aktivitetsInformasjonRepository.hentAktivitetsInformasjon(any(), any(), any()) } returns listOf()
        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)
        val periodeTilIGår = Periode(sak.rettighetsperiode.fom, LocalDate.now().minusDays(1))

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(4)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(periodeTilIGår)
    }

    @Test
    fun `skal hente ut aktuelle meldekort når alt unntatt siste meldekortperiode er fylt ut`() {
        val aktivitetsInformasjonPerioder = meldeperioder.filter { it.tom.plusWeeks(2) <= LocalDate.now() }
        val aktivitetsInformasjon = utledArbeidedeTimer(aktivitetsInformasjonPerioder)
        every { aktivitetsInformasjonRepository.hentAktivitetsInformasjon(any(), any(), any()) } returns aktivitetsInformasjon

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)
        val sistePeriodeTilIGår = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().minusDays(1))

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(sistePeriodeTilIGår)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(sistePeriodeTilIGår)
    }


    @Test
    fun `skal hente ut aktuelle meldekort hvor eldste ikke er fylt ut, men resten frem til i dag er utfylt (utvidelse av rettighetsperiode)`() {
        val aktivitetsInformasjonPerioder = meldeperioder.filter { it.fom >= sak.rettighetsperiode.fom.plusWeeks(2) && it.tom <= LocalDate.now() }
        val aktivitetsInformasjon = utledArbeidedeTimer(aktivitetsInformasjonPerioder)
        every { aktivitetsInformasjonRepository.hentAktivitetsInformasjon(any(), any(), any()) } returns aktivitetsInformasjon

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(førstePeriode)
    }

    @Test
    fun `skal hente ut aktuelle meldekort hvor eldste er delvis fylt ut, mens resten frem til i dag er utfylt (utvidelse av rettighetsperiode)`() {
        val aktivitetsInformasjonPerioder = meldeperioder.filter { it.fom >= sak.rettighetsperiode.fom.plusDays(6) && it.tom <= LocalDate.now() }
        val aktivitetsInformasjon = utledArbeidedeTimer(aktivitetsInformasjonPerioder)
        every { aktivitetsInformasjonRepository.hentAktivitetsInformasjon(any(), any(), any()) } returns aktivitetsInformasjon

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(førstePeriode)
    }

    private fun utledArbeidedeTimer(
        aktivitetsInformasjonPerioder: List<Periode>,
    ): List<AktivitetsInformasjon> {
        val utfyllingReferanse = UtfyllingReferanse(UUID.randomUUID())

        val aktivitetsInformasjon = aktivitetsInformasjonPerioder.flatMap { periode ->
            periode.iterator().asSequence().map { dag ->
                AktivitetsInformasjon(
                    registreringstidspunkt = clock.instant(),
                    utfylling = utfyllingReferanse,
                    fagsak = sak.referanse,
                    dato = dag,
                    timerArbeidet = 0.0,
                    fravær = null
                )
            }
        }
        return aktivitetsInformasjon
    }
}