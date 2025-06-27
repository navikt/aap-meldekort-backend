package no.nav.aap.kelvin

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.opplysningsplikt.TimerArbeidet
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
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

    val timerArbeidetRepository = mockk<TimerArbeidetRepository>()
    val kelvinSakRepository = mockk<KelvinSakRepository>()
    val clock = Clock.systemDefaultZone()

    val sakService = KelvinSakService(
        kelvinSakRepository = kelvinSakRepository,
        timerArbeidetRepository = timerArbeidetRepository,
        clock = clock
    )
    val meldeperiodeFlate = KelvinMeldeperiodeFlate(
        sakService = sakService,
        kelvinSakRepository = kelvinSakRepository,
        timerArbeidetRepository = timerArbeidetRepository,
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
        every { timerArbeidetRepository.hentTimerArbeidet(any(), any(), any()) } returns listOf()
        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)
        val periodeTilIGår = Periode(sak.rettighetsperiode.fom, LocalDate.now().minusDays(1))

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(4)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(periodeTilIGår)
    }

    @Test
    fun `skal hente ut aktuelle meldekort når alt unntatt siste meldekortperiode er fylt ut`() {
        val timerArbeidetPerioder = meldeperioder.filter { it.tom.plusWeeks(2) <= LocalDate.now() }
        val timerArbeidet = utledArbeidedeTimer(timerArbeidetPerioder)
        every { timerArbeidetRepository.hentTimerArbeidet(any(), any(), any()) } returns timerArbeidet

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)
        val sistePeriodeTilIGår = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().minusDays(1))

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(sistePeriodeTilIGår)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(sistePeriodeTilIGår)
    }


    @Test
    fun `skal hente ut aktuelle meldekort hvor eldste ikke er fylt ut, men resten frem til i dag er utfylt (utvidelse av rettighetsperiode)`() {
        val timerArbeidetPerioder = meldeperioder.filter { it.fom >= sak.rettighetsperiode.fom.plusWeeks(2) && it.tom <= LocalDate.now() }
        val timerArbeidet = utledArbeidedeTimer(timerArbeidetPerioder)
        every { timerArbeidetRepository.hentTimerArbeidet(any(), any(), any()) } returns timerArbeidet

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(førstePeriode)
    }

    @Test
    fun `skal hente ut aktuelle meldekort hvor eldste er delvis fylt ut, mens resten frem til i dag er utfylt (utvidelse av rettighetsperiode)`() {
        val timerArbeidetPerioder = meldeperioder.filter { it.fom >= sak.rettighetsperiode.fom.plusDays(6) && it.tom <= LocalDate.now() }
        val timerArbeidet = utledArbeidedeTimer(timerArbeidetPerioder)
        every { timerArbeidetRepository.hentTimerArbeidet(any(), any(), any()) } returns timerArbeidet

        val aktuellePerioder = meldeperiodeFlate.aktuelleMeldeperioder(innloggetBruker)

        assertThat(aktuellePerioder.antallUbesvarteMeldeperioder).isEqualTo(1)
        assertThat(aktuellePerioder.nesteMeldeperiode?.meldeperioden).isEqualTo(førstePeriode)
        assertThat(aktuellePerioder.manglerOpplysninger).isEqualTo(førstePeriode)
    }

    private fun utledArbeidedeTimer(
        timerArbeidetPerioder: List<Periode>,
    ): List<TimerArbeidet> {
        val utfyllingReferanse = UtfyllingReferanse(UUID.randomUUID())

        val timerArbeidet = timerArbeidetPerioder.flatMap { periode ->
            periode.iterator().asSequence().map { dag ->
                TimerArbeidet(
                    registreringstidspunkt = clock.instant(),
                    utfylling = utfyllingReferanse,
                    fagsak = sak.referanse,
                    dato = dag,
                    timerArbeidet = 0.0
                )
            }
        }
        return timerArbeidet
    }
}