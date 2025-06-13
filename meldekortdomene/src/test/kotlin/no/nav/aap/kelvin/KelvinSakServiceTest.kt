package no.nav.aap.kelvin

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.Fagsaknummer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import org.assertj.core.api.Assertions.assertThat
import java.time.*

class KelvinSakServiceTest {
    val innloggetBruker = InnloggetBruker(Ident("12345678901"), "test-token")

    val timerArbeidetRepository = mockk<TimerArbeidetRepository>()
    val kelvinSakRepository = mockk<KelvinSakRepository>()
    val clock = Clock.systemDefaultZone()

    val totalPeriode = Periode(LocalDate.now(clock).minusWeeks(2), LocalDate.now(clock).plusWeeks(8))

    val sak = KelvinSak(
        saksnummer = Fagsaknummer("1234"),
        status = KelvinSakStatus.LØPENDE,
        rettighetsperiode = totalPeriode,
    )

    @BeforeEach
    fun setUp() {
        every { kelvinSakRepository.hentSak(any(), any()) } returns sak
    }

    @Test
    fun `skal ikke ha frist hvis perioden ikke har vært en del av en periode med meldeplikt`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 3, 16, 12, 3, 0).toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        // gul
        val opplysningsperiode = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 5, 25))
        val sakService = KelvinSakService(
            kelvinSakRepository = kelvinSakRepository,
            timerArbeidetRepository = timerArbeidetRepository,
            clock = clock
        )

        // grønn
        every { kelvinSakRepository.hentMeldeplikt(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom.plusWeeks(4),
            opplysningsperiode.tom
        ).slidingWindow(
            size = 8,
            step = 14,
            partialWindows = true
        )

        // rød
        every { kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom,
            opplysningsperiode.tom
        ).slidingWindow(
            size = 14,
            step = 14,
            partialWindows = true
        )

        val meldeperiode = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 16))
        assertThat(sakService.finnMeldepliktfristForPeriode(innloggetBruker.ident, sak.referanse, meldeperiode)).isNull()
    }

    @Test
    fun `skal ha frist når perioden har vært en del av en periode med meldeplikt`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 3, 27, 12, 3, 0).toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        // gul
        val opplysningsperiode = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 5, 25))
        val sakService = KelvinSakService(
            kelvinSakRepository = kelvinSakRepository,
            timerArbeidetRepository = timerArbeidetRepository,
            clock = clock
        )

        // grønn
        every { kelvinSakRepository.hentMeldeplikt(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom.plusWeeks(4),
            opplysningsperiode.tom
        ).slidingWindow(
            size = 8,
            step = 14,
            partialWindows = true
        )

        // rød
        every { kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom,
            opplysningsperiode.tom
        ).slidingWindow(
            size = 14,
            step = 14,
            partialWindows = true
        )

        val meldeperiode = Periode(LocalDate.of(2025, 3, 17), LocalDate.of(2025, 3, 30))
        assertThat(sakService.finnMeldepliktfristForPeriode(innloggetBruker.ident, sak.referanse, meldeperiode)).isEqualTo(LocalDate.of(2025, 4, 7).atTime(23, 59))
    }

    @Test
    fun `ser på en periode tilbake i tid som har hatt meldeplikt, skal ha frist`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 5, 25, 12, 3, 0).toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        // gul
        val opplysningsperiode = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 5, 25))
        val sakService = KelvinSakService(
            kelvinSakRepository = kelvinSakRepository,
            timerArbeidetRepository = timerArbeidetRepository,
            clock = clock
        )

        // grønn
        every { kelvinSakRepository.hentMeldeplikt(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom.plusWeeks(4),
            opplysningsperiode.tom
        ).slidingWindow(
            size = 8,
            step = 14,
            partialWindows = true
        )

        // rød
        every { kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.saksnummer) } returns Periode(
            opplysningsperiode.fom,
            opplysningsperiode.tom
        ).slidingWindow(
            size = 14,
            step = 14,
            partialWindows = true
        )

        val meldeperiode = Periode(LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 27))
        assertThat(sakService.finnMeldepliktfristForPeriode(innloggetBruker.ident, sak.referanse, meldeperiode)).isEqualTo(LocalDate.of(2025, 5, 5).atTime(23, 59))
    }
}