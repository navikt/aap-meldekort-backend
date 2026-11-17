package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.meldeperiode.erDekketAvFritak
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.utfylling.AktivitetsInformasjon
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class KelvinSakService(
    private val kelvinSakRepository: KelvinSakRepository,
    private val aktivitetsInformasjonRepository: AktivitetsInformasjonRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, clock: Clock) : this(
        aktivitetsInformasjonRepository = repositoryProvider.provide(),
        kelvinSakRepository = repositoryProvider.provide(),
        clock = clock,
    )

    fun hentMeldeperioder(sak: FagsakReferanse): List<Meldeperiode> {
        val opplysningsbehov = kelvinSakRepository.hentOpplysningsbehov(sak.nummer)
        val meldeperioder = kelvinSakRepository.hentMeldeperioder(sak.nummer)

        fun harOpplysningsbehov(dag: LocalDate): Boolean {
            return opplysningsbehov.any { periode -> dag in periode }
        }

        return meldeperioder
            .mapNotNull { meldeperiode ->
                val opplysningsperiode = meldeperiode
                    .dropWhile { dag -> !harOpplysningsbehov(dag) }
                    .dropLastWhile { dag -> !harOpplysningsbehov(dag) }
                    .let { periode ->
                        if (periode.isEmpty())
                            null
                        else
                            Periode(periode.first(), periode.last())
                    }
                    ?: return@mapNotNull null
                Meldeperiode(
                    meldeperioden = opplysningsperiode,
                    /* TODO: behandlingsflyt burde bestemme hva meldevinduet er. */
                    meldevindu = Periode(
                        tidligsteInnsendingstidspunktMeldedag(meldeperiode.tom.plusDays(1)),
                        meldeperiode.tom.plusDays(8)
                    ),
                )
            }
    }

    fun antallUbesvarteMeldeperioder(ident: Ident, sak: FagsakReferanse): Int {
        val meldeperioderUtenInnsending = meldeperioderUtenInnsending(ident, sak)
        val meldeperioderUtenInnsendingSomKanSendesInn = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= LocalDate.now(clock) }

        return meldeperioderUtenInnsendingSomKanSendesInn.size
    }


    fun registrerteAktivitetsInformasjon(
        ident: Ident,
        sak: FagsakReferanse,
        periode: Periode
    ): List<AktivitetsInformasjon> {
        val registrerteOpplysninger =
            aktivitetsInformasjonRepository.hentAktivitetsInformasjon(ident, sak, periode)
                .map { AktivitetsInformasjon(dato = it.dato, timer = it.timerArbeidet, fravær = it.fravær) }
                .toMutableList()

        val aktivitetsInformasjon = sequence {
            for (dato in periode) {
                if (registrerteOpplysninger.firstOrNull()?.dato == dato) {
                    yield(registrerteOpplysninger.removeFirst())
                } else {
                    yield(AktivitetsInformasjon(dato, null))
                }
            }
        }
        return aktivitetsInformasjon.toList()
    }

    /**
     * Meldeperioder hvor medlemmet mangler innsending og må gi opplysninger.
     *
     * Perioder som er fullt dekket av fritak fra meldeplikt er utelatt: behandlingsflyt antar 0 timer
     * for disse, så medlemmet kan sende inn, men må ikke.
     */
    fun meldeperioderUtenInnsending(ident: Ident, sak: FagsakReferanse): List<Meldeperiode> {
        val perioder = hentMeldeperioder(sak)
        val fritak = kelvinSakRepository.hentFritakMeldeplikt(sak.nummer)
        val timerRegistrert = timerRegistrertForMeldeperiodene(perioder, ident, sak)

        val (fritatte, påkrevde) = perioder
            .filter { periode ->
                periode.meldeperioden.any { dag -> timerRegistrert.none { it.dato == dag } }
            }
            .partition { it.erDekketAvFritak(fritak) }

        val antallFritatteMedÅpentMeldevindu = fritatte.count { it.meldevindu.fom <= LocalDate.now(clock) }
        if (antallFritatteMedÅpentMeldevindu > 0) {
            log.info(
                "Hopper over $antallFritatteMedÅpentMeldevindu meldeperioder uten innsending for sak " +
                        "${sak.nummer.asString} fordi medlemmet har fritak fra meldeplikt"
            )
        }

        return påkrevde
    }

    private fun timerRegistrertForMeldeperiodene(
        perioder: List<Meldeperiode>,
        ident: Ident,
        sak: FagsakReferanse
    ): List<no.nav.aap.opplysningsplikt.AktivitetsInformasjon> {
        val tidligsteFom = perioder.minByOrNull { it.meldeperioden.fom }?.meldeperioden?.fom ?: Tid.MIN
        val senesteTom = perioder.maxByOrNull { it.meldeperioden.tom }?.meldeperioden?.tom ?: Tid.MAKS
        val timerPeriode = Periode(fom = tidligsteFom, tom = senesteTom)

        return aktivitetsInformasjonRepository.hentAktivitetsInformasjon(ident, sak, timerPeriode)
    }

    fun finnMeldepliktfristForPeriode(sak: FagsakReferanse, periode: Periode): LocalDateTime? {
        val meldepliktperioder = kelvinSakRepository.hentMeldeplikt(sak.nummer)
        val muligForlengetMeldefrist = senesteInnsendingstidspunktMeldefrist(periode.tom.plusDays(8))

        if (meldepliktperioder.any { it.overlapper(Periode(periode.tom.plusDays(1), muligForlengetMeldefrist)) }) {
            return muligForlengetMeldefrist.atTime(23, 59)
        }
        return null
    }
}
