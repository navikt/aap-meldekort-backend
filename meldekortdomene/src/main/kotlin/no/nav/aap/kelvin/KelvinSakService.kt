package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.utfylling.TimerArbeidet
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class KelvinSakService(
    private val kelvinSakRepository: KelvinSakRepository,
    private val timerArbeidetRepository: TimerArbeidetRepository,
    private val clock: Clock,
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        timerArbeidetRepository = repositoryProvider.provide(),
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
                    meldevindu = Periode(meldeperiode.tom.plusDays(1), meldeperiode.tom.plusDays(8)),
                )
            }
    }

    fun antallUbesvarteMeldeperioder(ident: Ident, sak: FagsakReferanse): Int {
        val meldeperioderUtenInnsending = meldeperioderUtenInnsending(ident, sak)
        val meldeperioderUtenInnsendingSomKanSendesInn = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= LocalDate.now(clock) }

        return meldeperioderUtenInnsendingSomKanSendesInn.size
    }


    fun registrerteTimerArbeidet(
        ident: Ident,
        sak: FagsakReferanse,
        periode: Periode
    ): List<TimerArbeidet> {
        val registrerteOpplysninger =
            timerArbeidetRepository.hentTimerArbeidet(ident, sak, periode)
                .map { TimerArbeidet(dato = it.dato, timer = it.timerArbeidet) }
                .toMutableList()

        val timerArbeidet = sequence {
            for (dato in periode) {
                if (registrerteOpplysninger.firstOrNull()?.dato == dato) {
                    yield(registrerteOpplysninger.removeFirst())
                } else {
                    yield(TimerArbeidet(dato, null))
                }
            }
        }
        return timerArbeidet.toList()
    }

    fun meldeperioderUtenInnsending(ident: Ident, sak: FagsakReferanse): List<Meldeperiode> {
        val perioder = hentMeldeperioder(sak)
        val timerRegistrert = timerRegistrertForMeldeperiodene(perioder, ident, sak)
        return perioder.filter { periode ->
            for (meldeperiodeDag in periode.meldeperioden) {
                val erRegistrertTimerForDag = timerRegistrert.any { it.dato == meldeperiodeDag }
                if (!erRegistrertTimerForDag) {
                    return@filter true
                }
            }
            false
        }
    }

    private fun timerRegistrertForMeldeperiodene(
        perioder: List<Meldeperiode>,
        ident: Ident,
        sak: FagsakReferanse
    ): List<no.nav.aap.opplysningsplikt.TimerArbeidet> {
        val tidligsteFom = perioder.minByOrNull { it.meldeperioden.fom }?.meldeperioden?.fom ?: LocalDate.MIN
        val senesteTom = perioder.maxByOrNull { it.meldeperioden.tom }?.meldeperioden?.tom ?: LocalDate.MAX
        val timerPeriode = Periode(fom = tidligsteFom, tom = senesteTom)

        return timerArbeidetRepository.hentTimerArbeidet(ident, sak, timerPeriode)
    }

    fun finnMeldepliktfristForPeriode(sak: FagsakReferanse, periode: Periode): LocalDateTime? {
        val meldepliktperioder = kelvinSakRepository.hentMeldeplikt(sak.nummer)
        if (meldepliktperioder.any { it.overlapper(Periode(periode.tom.plusDays(1), periode.tom.plusDays(8))) }) {
            return periode.tom.plusDays(8).atTime(23, 59)
        }
        return null
    }

}
