package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.AapFlyt
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate

class KelvinService(
    override val sak: Sak,
    private val timerArbeidetRepository: TimerArbeidetRepository,
) : FagsystemService {
    override val innsendingsflyt = AapFlyt(timerArbeidetRepository)
    override val korrigeringsflyt = AapFlyt(timerArbeidetRepository)

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        val senesteOpplysningsdato =
            timerArbeidetRepository.hentSenesteOpplysningsdato(innloggetBruker.ident, sak.referanse)
                ?: LocalDate.MIN

        /* TODO: behandlingsflyt bestemmer hva meldeperiodene er. */
        val perioder = Periode(sak.rettighetsperiode.fom, LocalDate.now()).slidingWindow(
            size = 14,
            step = 14,
            partialWindows = true
        )
            .map {
                Meldeperiode(
                    meldeperioden = it,
                    meldevindu = Periode(it.tom.plusDays(1), it.tom.plusDays(8)),
                )
            }

        val meldeperioderUtenInnsending =
            perioder.dropWhile { it.meldeperioden.tom <= senesteOpplysningsdato }
        val meldeperioderUtenInnsendingSomKanSendesInn = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= LocalDate.now() }

        return FagsystemService.VentendeOgNeste(
            ventende = meldeperioderUtenInnsendingSomKanSendesInn,
            neste = meldeperioderUtenInnsendingSomKanSendesInn.firstOrNull()
                ?: meldeperioderUtenInnsending.firstOrNull(),
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val perioder = Periode(sak.rettighetsperiode.fom, LocalDate.now()).slidingWindow(
            size = 14,
            step = 14,
            partialWindows = true
        )
            .map {
                Meldeperiode(
                    meldeperioden = it,
                    meldevindu = Periode(it.tom.plusDays(1), it.tom.plusDays(8)),
                )
            }

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
    }

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer {
        /* TODO: må håndtere at det ikke er registrert noen timer, og da returnere tomt for den
         * dagen.
         */
        val timerArbeidet = timerArbeidetRepository.hentTimerArbeidet(innloggetBruker.ident, sak.referanse, periode)
            .map { TimerArbeidet(dato = it.dato, timer = it.timerArbeidet) }
        return FagsystemService.PeriodeDetaljer(
            periode = periode,
            svar = Svar(
                svarerDuSant = true, /* TODO */
                harDuJobbet = true, /* TODO */
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = true, /* TODO */
            )
        )
    }

    override fun forberedVanligFlyt(
        innloggetBruker: InnloggetBruker,
        periode: Periode,
        utfyllingReferanse: UtfyllingReferanse
    ) {
    }

    override fun forberedKorrigeringFlyt(
        innloggetBruker: InnloggetBruker,
        periode: Periode,
        utfyllingReferanse: UtfyllingReferanse
    ) {
    }

    override fun hentHistoriskeSvar(innloggetBruker: InnloggetBruker, periode: Periode): Svar {
        /* TODO: må håndtere at det ikke er registrert noen timer, og da returnere tomt for den
         * dagen.
         */
        val timerArbeidet = timerArbeidetRepository.hentTimerArbeidet(innloggetBruker.ident, sak.referanse, periode)
            .map { TimerArbeidet(dato = it.dato, timer = it.timerArbeidet) }
        return Svar(
            svarerDuSant = null,
            harDuJobbet = null,
            timerArbeidet =
                timerArbeidet,
            stemmerOpplysningene = null,
        )
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): KelvinService {
            return KelvinService(
                sak = sak,
                timerArbeidetRepository = RepositoryProvider(connection).provide(),
            )
        }
    }
}
