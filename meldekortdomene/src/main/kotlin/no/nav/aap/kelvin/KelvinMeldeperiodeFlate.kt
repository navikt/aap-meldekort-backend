package no.nav.aap.kelvin

import java.time.LocalDate
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.utfylling.Svar

class KelvinMeldeperiodeFlate(
    private val sakService: KelvinSakService,
    private val kelvinSakRepository: KelvinSakRepository,
    private val timerArbeidetRepository: TimerArbeidetRepository,
) : MeldeperiodeFlate {

    override fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): MeldeperiodeFlate.KommendeMeldeperioder {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, LocalDate.now())
            ?: return MeldeperiodeFlate.KommendeMeldeperioder(
                antallUbesvarteMeldeperioder = 0,
                manglerOpplysninger = null,
                nesteMeldeperiode = null,
            )

        val senesteOpplysningsdato = timerArbeidetRepository
            .hentSenesteOpplysningsdato(innloggetBruker.ident, sak.referanse)
            ?: LocalDate.MIN

        val perioder = sakService.hentMeldeperioder(innloggetBruker.ident, sak.referanse)

        val meldeperioderUtenInnsending =
            perioder.dropWhile { it.meldeperioden.tom <= senesteOpplysningsdato }

        val ventende = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= LocalDate.now() }

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
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, LocalDate.now()) ?: return emptyList()

        val perioder = sakService.hentMeldeperioder(innloggetBruker.ident, sak.referanse)

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


    companion object {
        fun konstruer(connection: DBConnection): KelvinMeldeperiodeFlate {
            val repositoryProvider = RepositoryProvider(connection)
            return KelvinMeldeperiodeFlate(
                sakService = KelvinSakService.konstruer(connection),
                kelvinSakRepository = repositoryProvider.provide(),
                timerArbeidetRepository = repositoryProvider.provide(),
            )
        }
    }
}
