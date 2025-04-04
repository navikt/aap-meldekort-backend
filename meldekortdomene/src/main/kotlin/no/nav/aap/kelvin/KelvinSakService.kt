package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.utfylling.TimerArbeidet
import java.time.LocalDate

class KelvinSakService(
    private val kelvinSakRepository: KelvinSakRepository,
    private val timerArbeidetRepository: TimerArbeidetRepository,
)  {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        timerArbeidetRepository = repositoryProvider.provide(),
        kelvinSakRepository = repositoryProvider.provide(),
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
        val senesteOpplysningsdato =
            timerArbeidetRepository.hentSenesteOpplysningsdato(ident, sak)
                ?: LocalDate.MIN

        val perioder = hentMeldeperioder(ident, sak)

        val meldeperioderUtenInnsending =
            perioder.dropWhile { it.meldeperioden.tom <= senesteOpplysningsdato }
        val meldeperioderUtenInnsendingSomKanSendesInn = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= LocalDate.now() }

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
}
