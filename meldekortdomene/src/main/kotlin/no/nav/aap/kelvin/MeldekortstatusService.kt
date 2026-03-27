package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.FagsakReferanse
import java.time.Clock
import java.time.LocalDate

class MeldekortstatusService(
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

    fun hentMeldekortstatus(ident:Ident): Meldekortstatus? {
        val sak = kelvinSakRepository.hentSak(ident, LocalDate.now(clock)) ?: return null
        return Meldekortstatus(
            harInnsendteMeldekort = harInnsendteMeldekort(ident, sak.referanse),
            meldekortTilUtfylling = hentMeldekortTilUtfylling(ident, sak.referanse),
        )
    }

    private fun hentMeldekortTilUtfylling(ident: Ident, referanse: FagsakReferanse): List<MeldekortTilUtfylling> {
        val meldeperioderUtenInnsending =
            kelvinSakService.meldeperioderUtenInnsending(ident, referanse)

        return meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.fom <= LocalDate.now(clock) }
            .map { meldeperiode ->
                MeldekortTilUtfylling(
                    // vi åpner ikke innsending på et spesielt tidspunkt, man kan alltid kun fylle ut det neste meldekortet som skal sendes inn, kanFyllesUtFra settes derfor alltid til null
                    kanFyllesUtFra = null,
                    kanSendesFra = meldeperiode.meldevindu.fom,
                    fristForInnsending = meldeperiode.meldevindu.tom
                )
            }
    }

    private fun harInnsendteMeldekort(ident: Ident, referanse: FagsakReferanse): Boolean {
        val perioder = kelvinSakService.hentMeldeperioder(referanse)

        val senesteOpplysningsdato =
            aktivitetsInformasjonRepository.hentSenesteOpplysningsdato(ident, referanse)
                ?: LocalDate.MIN

        return perioder.any { it.meldeperioden.fom <= senesteOpplysningsdato }
    }
}
