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

    val fastsattePerioder = Periode(sak.rettighetsperiode.fom, LocalDate.now().plusWeeks(1)).slidingWindow(
        size = 8,
        step = 14,
        partialWindows = true
    )

    val meldeperioder = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom).slidingWindow(
        size = 14,
        step = 14,
        partialWindows = true
    )


    @BeforeEach
    fun setUp() {
        every { kelvinSakRepository.hentMeldeperioder(any(), any()) } returns meldeperioder
        every { kelvinSakRepository.hentMeldeplikt(any(), any()) } returns fastsattePerioder
        every { timerArbeidetRepository.hentSenesteOpplysningsdato(any(), any()) } returns LocalDate.MIN
        every { kelvinSakRepository.hentSak(any(), any()) } returns sak
    }

//    @Test
//    fun `frist er i slutten av meldeperiode + 8 dager når vi er i en meldeperiode og bruker ikke allerede har meldt seg`() {
//        val fristForInnsending =
//            sakService.finnTidspunktForOpplysningsbehov(innloggetBruker.ident, sak.referanse)
//
//        assertThat(fristForInnsending).isEqualTo(fastsattePerioder.last().tom.atTime(23, 59));
//    }

//    @Test
//    fun `frist er i dag når vi er utenfor alle fastsatte meldeperioder og bruker ikke har meldt seg`() {
//        val rettighetsperiode = Periode(LocalDate.now().minusDays(9).minusWeeks(2), LocalDate.now().plusWeeks(8))
//        every { kelvinSakRepository.hentMeldeplikt(any(), any()) } returns Periode(rettighetsperiode.fom, rettighetsperiode.tom).slidingWindow(
//            size = 8,
//            step = 14,
//            partialWindows = true
//        )
//        val fristForInnsending =
//            sakService.finnTidspunktForOpplysningsbehov(innloggetBruker.ident, sak.referanse)
//
//        assertThat(fristForInnsending).isEqualTo(LocalDate.now(clock).atTime(23, 59));
//    }

//    @Test
//    fun `når dd er utenfor alle meldepliktperioder og det finnes en tidligere periode som hører til dagens meldeperiode og seneste opplysningsdato er tidligere en denne så må bruker melde seg nå med en gang`() {
//        val rettighetsperiode = Periode(LocalDate.now().minusDays(9).minusWeeks(2), LocalDate.now().plusWeeks(8))
//        every { kelvinSakRepository.hentMeldeplikt(any(), any()) } returns Periode(rettighetsperiode.fom, rettighetsperiode.tom).slidingWindow(
//            size = 8,
//            step = 14,
//            partialWindows = true
//        )
//        every { timerArbeidetRepository.hentSenesteOpplysningsdato(any(), any()) } returns rettighetsperiode.fom.plusDays(4)
//        assertThat(sakService.fristErOversittet(innloggetBruker.ident, sak.referanse)).isTrue()
//    }

//    @Test
//    fun `når dd er er innenfor en meldepliktperiode må ikke bruker melde seg umiddelbart`() {
//        val rettighetsperiode = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(1))
//        every { kelvinSakRepository.hentMeldeplikt(any(), any()) } returns Periode(rettighetsperiode.fom, rettighetsperiode.tom).slidingWindow(
//            size = 8,
//            step = 14,
//            partialWindows = true
//        )
//        every { timerArbeidetRepository.hentSenesteOpplysningsdato(any(), any()) } returns rettighetsperiode.tom.minusWeeks(1)
//        assertThat(sakService.fristErOversittet(innloggetBruker.ident, sak.referanse)).isFalse()
//    }

    @Test
    fun `innbygger har søkt om AAP men ikke fått vedtak enda, har ingen melde- eller opplysningsplikt`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 4, 1, 12, 3, 0).toInstant(ZoneOffset.UTC)),
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

        every { kelvinSakRepository.hentSak(any(), any()) } returns KelvinSak(
            saksnummer = Fagsaknummer("1234"),
            status = KelvinSakStatus.UTREDES,
            rettighetsperiode = totalPeriode,
        )

        val resultat = sakService.finnMeldepliktFrist(ident = innloggetBruker.ident, sak = sak.referanse)
        assertThat(resultat).isNull()
    }

    @Test
    fun `har fått innvilget aap tilbake i tid, ser på meldekort fra første periode, skal ikke ha frist for meldeplikt på dette kortet`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 4, 1, 12, 3, 0).toInstant(ZoneOffset.UTC)),
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

        // frist for meldeplikt ligger frem i tid (langt forbi denne perioden), og vi ønsker ikke å vise en frist her
        // det er fortsatt et opplysningsbehov her, så bruker må sende opplysninger så fort som mulig for å unngå at utbetaling stanses
        // fristForMeldeplikt -> langt frem i tid og ikke relevant
        // fristForOpplysninger -> i dag

        println("NÅ:\t\t\t\t" + LocalDate.now(clock))
        println("MELDEPLIKT:\t\t" + kelvinSakRepository.hentMeldeplikt(innloggetBruker.ident, sak.saksnummer))
        println("MELDEPERIODE:\t" + kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.saksnummer))
    }

    @Test
    fun `står i slutten av en meldeperiode, og har oppfylt tidligere meldeplikt, skal da få frist på slutten av meldepliktperioden`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 3, 30, 12, 3, 0).toInstant(ZoneOffset.UTC)),
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

        // står i slutten av en meldeperiode, tidligere meldeplikt er oppfylt. Skal få frist på slutten av meldeplikt-perioden (7.4.2025)
        val resultat = sakService.finnMeldepliktFrist(innloggetBruker.ident, sak.referanse)
        assertThat(resultat).isNotNull()
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 4, 7))
    }

    @Test
    fun `meldeperioden er passert, men er innenfor meldepliktperioden, skal få frist i slutten av meldepliktperioden`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 4, 2, 12, 3, 0).toInstant(ZoneOffset.UTC)),
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

        // meldeperioden er passert, men er fortsatt innenfor meldepliktperioden. Skal få frist i slutten av meldeplikt-perioden
        val resultat = sakService.finnMeldepliktFrist(innloggetBruker.ident, sak.referanse)
        assertThat(resultat).isNotNull()
        assertThat(resultat).isEqualTo(LocalDate.of(2025, 4, 7))
    }

    @Test
    fun `meldepliktperioden er passert, frist for meldeplikt skal være i dag`() {
        val clock = Clock.fixed(
            Instant.from(LocalDateTime.of(2025, 4, 8, 12, 3, 0).toInstant(ZoneOffset.UTC)),
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

        // meldepliktperioden er passert, frist skal derfor være i dag
        println("NÅ:\t\t\t\t" + LocalDate.now(clock))
        println("MELDEPLIKT:\t\t" + kelvinSakRepository.hentMeldeplikt(innloggetBruker.ident, sak.saksnummer))
        println("MELDEPERIODE:\t" + kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.saksnummer))
    }
}