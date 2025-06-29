package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.utfylling.Svar
import java.time.Clock
import java.time.LocalDate

class KelvinMeldeperiodeFlate(
    private val sakService: KelvinSakService,
    private val kelvinSakRepository: KelvinSakRepository,
    private val timerArbeidetRepository: TimerArbeidetRepository,
    private val clock: Clock,
) : MeldeperiodeFlate {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        sakService = KelvinSakService(repositoryProvider, gatewayProvider, clock),
        kelvinSakRepository = repositoryProvider.provide(),
        timerArbeidetRepository = repositoryProvider.provide(),
        clock = clock,
    )

    override fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): MeldeperiodeFlate.KommendeMeldeperioder {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, LocalDate.now(clock))
            ?: return MeldeperiodeFlate.KommendeMeldeperioder(
                antallUbesvarteMeldeperioder = 0,
                manglerOpplysninger = null,
                nesteMeldeperiode = null,
            )

        val meldeperioderUtenInnsending = sakService.meldeperioderUtenInnsending(innloggetBruker.ident, sak.referanse)

        val ventende = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.fom <= LocalDate.now(clock) }

        val manglerOpplysninger = ventende
            .takeIf { it.isNotEmpty() }
            ?.let {
                Periode(
                    fom = it.first().meldeperioden.fom,
                    tom = it.last().meldeperioden.tom,
                )
            }

        return MeldeperiodeFlate.KommendeMeldeperioder(
            antallUbesvarteMeldeperioder = ventende.size,
            manglerOpplysninger = manglerOpplysninger,
            nesteMeldeperiode = ventende.firstOrNull() ?: meldeperioderUtenInnsending.firstOrNull(),
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<MeldeperiodeFlate.HistoriskMeldeperiode> {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, LocalDate.now(clock)) ?: return emptyList()

        val perioder = sakService.hentMeldeperioder(sak.referanse)

        val senesteOpplysningsdato =
            timerArbeidetRepository.hentSenesteOpplysningsdato(innloggetBruker.ident, sak.referanse)
                ?: LocalDate.MIN

        /* Her ønsker vi å liste opp meldeperioder hvor:
         * - medlemmet har gitt opplysninger
         * - medlemmet har ikke gitt opplysninger, men er heller ikke pliktig til å gi opplysninger (fritak, f.eks.)
         */

        return perioder
            .filter { it.meldeperioden.fom <= senesteOpplysningsdato }
            .reversed()
            .map {
                val detaljer = periodedetaljer(innloggetBruker, it.meldeperioden)
                MeldeperiodeFlate.HistoriskMeldeperiode(
                    meldeperiode = it,
                    totaltAntallTimerIPerioden = detaljer.svar.timerArbeidet.sumOf { it.timer ?: 0.0 }
                )
            }
    }

    override fun periodedetaljer(
        innloggetBruker: InnloggetBruker,
        periode: Periode
    ): MeldeperiodeFlate.PeriodeDetaljer {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, periode.fom) ?: error("ingen sak i perioden")
        val timerArbeidet = sakService.registrerteTimerArbeidet(innloggetBruker.ident, sak.referanse, periode)
        return MeldeperiodeFlate.PeriodeDetaljer(
            periode = periode,
            svar = Svar(
                svarerDuSant = true, /* TODO */
                harDuJobbet = true, /* TODO */
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = true, /* TODO */
            )
        )
    }
}
