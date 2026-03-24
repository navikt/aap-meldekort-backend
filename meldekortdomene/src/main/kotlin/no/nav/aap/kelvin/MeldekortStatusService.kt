package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.FagsakReferanse
import java.time.Clock
import java.time.LocalDate

class MeldekortStatusService(
    private val kelvinSakRepository: KelvinSakRepository,
    private val aktivitetsInformasjonRepository: AktivitetsInformasjonRepository,
    private val kelvinSakService: KelvinSakService,
    private val clock: Clock,
) {
    constructor(repositoryProvider: RepositoryProvider, clock: Clock) : this(
        kelvinSakRepository = repositoryProvider.provide(),
        aktivitetsInformasjonRepository = repositoryProvider.provide(),
        kelvinSakService = KelvinSakService(repositoryProvider, clock),
        clock = clock
    )

    fun brukerHarSakIKelvin(ident: Ident): KelvinSak? {
        return kelvinSakRepository.hentSak(ident, LocalDate.now(clock))
    }

    fun hentMeldekortTilUtfylling(ident: Ident, referanse: FagsakReferanse): List<MeldekortTilUtfylling> {
        val meldeperioderUtenInnsending =
            kelvinSakService.meldeperioderUtenInnsending(ident, referanse)

        return meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.fom <= LocalDate.now(clock) }
            .map { meldeperiode ->
                MeldekortTilUtfylling(
                    kanFyllesUtFra = null, // TODO sjekk om denne er relevant
                    kanSendesFra = meldeperiode.meldevindu.fom,
                    fristForInnsending = meldeperiode.meldevindu.tom
                )
            }
    }

    fun harInnsendteMeldekort(ident: Ident, referanse: FagsakReferanse): Boolean {
        val perioder = kelvinSakService.hentMeldeperioder(referanse)

        val senesteOpplysningsdato =
            aktivitetsInformasjonRepository.hentSenesteOpplysningsdato(ident, referanse)
                ?: LocalDate.MIN

        return perioder.any { it.meldeperioden.fom <= senesteOpplysningsdato }
    }
}
