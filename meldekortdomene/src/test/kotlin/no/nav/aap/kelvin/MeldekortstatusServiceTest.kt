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

class MeldekortstatusServiceTest {

    private val ident = Ident("12345678901")

    private val andreMars = LocalDate.of(2026, 3, 2)
    private val clock = Clock.fixed(
        Instant.from(andreMars.atStartOfDay(ZoneId.of("UTC")).toInstant()),
        ZoneId.of("UTC")
    )

    private val kelvinSakRepository = mockk<KelvinSakRepository>()
    private val aktivitetsInformasjonRepository = mockk<AktivitetsInformasjonRepository>()
    private val kelvinSakService = mockk<KelvinSakService>()

    private val service = MeldekortstatusService(
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
    fun `returnerer null når bruker ikke har en sak i Kelvin`() {
        every { kelvinSakRepository.hentSak(ident, andreMars) } returns null
        val resultat = service.hentMeldekortstatus(ident)
        assertThat(resultat).isNull()
    }

    @Test
    fun `bruker har historiske meldekort, men ingen til utfylling`() {
        every { kelvinSakRepository.hentSak(ident, andreMars) } returns sak
        every { kelvinSakService.hentMeldeperioder(sak.referanse) } returns listOf(
            meldeperiode(meldeperiodeFom = andreMars.minusMonths(1), meldeperiodeTom = andreMars.minusWeeks(2))
        )
        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns emptyList()
        every {
            aktivitetsInformasjonRepository.hentSenesteOpplysningsdato(
                ident,
                sak.referanse
            )
        } returns andreMars.minusWeeks(2).plusDays(1)

        val resultat = service.hentMeldekortstatus(ident)
        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isTrue()
        assertThat(resultat.meldekortTilUtfylling).isEmpty()
    }

    @Test
    fun `bruker har ingen historiske meldekort, men har meldekort til utfylling`() {
        val toUkerSiden = andreMars.minusDays(14)
        every { kelvinSakRepository.hentSak(ident, andreMars) } returns sak
        every { kelvinSakService.hentMeldeperioder(sak.referanse) } returns emptyList()
        every { aktivitetsInformasjonRepository.hentSenesteOpplysningsdato(ident, sak.referanse) } returns null
        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns listOf(
            meldeperiode(meldevinduFom = toUkerSiden, meldevinduTom = toUkerSiden.plusDays(7)),
            meldeperiode(meldevinduFom = andreMars, meldevinduTom = andreMars.plusDays(7)),
        )

        val resultat = service.hentMeldekortstatus(ident)
        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isFalse()
        assertThat(resultat.meldekortTilUtfylling).hasSize(2)
        assertThat(resultat.meldekortTilUtfylling.map { it.kanSendesFra }).containsExactly(toUkerSiden, andreMars)
    }

    @Test
    fun `meldekort hvor meldevindu starter i fremtiden tas ikke med i meldekort til utfylling`() {
        val toUkerSiden = andreMars.minusDays(14)

        every { kelvinSakRepository.hentSak(ident, andreMars) } returns sak
        every { kelvinSakService.hentMeldeperioder(sak.referanse) } returns emptyList()
        every { aktivitetsInformasjonRepository.hentSenesteOpplysningsdato(ident, sak.referanse) } returns null
        every { kelvinSakService.meldeperioderUtenInnsending(ident, sak.referanse) } returns listOf(
            meldeperiode(meldevinduFom = toUkerSiden, meldevinduTom = toUkerSiden.plusDays(7)),
            meldeperiode(meldevinduFom = andreMars.plusDays(1), meldevinduTom = andreMars.plusDays(7)),
        )

        val resultat = service.hentMeldekortstatus(ident)
        assertThat(resultat).isNotNull
        assertThat(resultat!!.harInnsendteMeldekort).isFalse()
        assertThat(resultat.meldekortTilUtfylling).hasSize(1)
        assertThat(resultat.meldekortTilUtfylling.map { it.kanSendesFra }).containsExactly(toUkerSiden)
    }

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
