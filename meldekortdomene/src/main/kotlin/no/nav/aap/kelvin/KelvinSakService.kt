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

    fun hentMeldeperioder(ident: Ident, sak: FagsakReferanse): List<Meldeperiode> {
        val opplysningsbehov = kelvinSakRepository.hentOpplysningsbehov(ident, sak.nummer)
        val meldeperioder = kelvinSakRepository.hentMeldeperioder(ident, sak.nummer)

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
        val perioder = hentMeldeperioder(ident, sak)
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


    // TODO vurder navn
    fun finnTidspunktForOpplysningsbehov(ident: Ident, sak:FagsakReferanse):LocalDateTime? {
        val senesteOpplysningsdato = timerArbeidetRepository.hentSenesteOpplysningsdato(ident, sak)?: LocalDate.MIN
        val akkuratNå = LocalDate.now(clock)
        val meldepliktperioder = kelvinSakRepository.hentMeldeplikt(ident, sak.nummer)

        meldepliktperioder.forEach {   // gå igjennom alle perioder
            // hva hvis senesteOpplysningsdato er i samme periode? Da er vel meldeplikten oppfylt og vi skal ikke vise en frist?
            if (akkuratNå in it.fom..it.tom && senesteOpplysningsdato !in it.fom..it.tom) { // hvis dd er i en periode med meldeplikt, og senesteOpplysningsdato ikke er i samme periode
                return it.tom.atTime(23, 59)
            }
        }
        return null
    }

    private fun dagensDatoFinnesIEnMeldepliktPeriode(dagensDato: LocalDate, meldepliktPerioder: List<Periode>): Boolean {
        meldepliktPerioder.forEach {
            if (dagensDato in it.fom..it.tom)
                return true
        }
        return false
    }

    fun fristErOversittet(ident: Ident, sak:FagsakReferanse):Boolean {
        val senesteOpplysningsdato = timerArbeidetRepository.hentSenesteOpplysningsdato(ident, sak)
        val akkuratNå = LocalDate.now(clock)
        val meldepliktperioder = kelvinSakRepository.hentMeldeplikt(ident, sak.nummer)
        val meldeperioder = kelvinSakRepository.hentMeldeperioder(ident, sak.nummer)

        val ddErIMeldeperiode = dagensDatoFinnesIEnMeldepliktPeriode(akkuratNå, meldepliktperioder)
        if (!ddErIMeldeperiode) {
            // må finne meldeperioden som gjelder for i dag og sjekke om den tilhører en tidligere periode
            val meldeperioden = meldeperioder.find { akkuratNå in it.fom..it.tom }
            if (meldeperioden != null) {
                val tidligereOverlappendeMeldepliktPeriode = meldepliktperioder.find { it.overlapper(meldeperioden) }
                return senesteOpplysningsdato != null && senesteOpplysningsdato <= tidligereOverlappendeMeldepliktPeriode?.fom
            }
        }
        return false
    }
}
