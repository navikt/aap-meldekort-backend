package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.Sak
import no.nav.aap.sak.AapGateway
import no.nav.aap.utfylling.AapFlyt
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate

class KelvinService(
    override val sak: Sak,
    private val aapGateway: AapGateway,
    private val timerArbeidetRepository: TimerArbeidetRepository,
) : FagsystemService {
    override val innsendingsflyt = AapFlyt(timerArbeidetRepository)
    override val korrigeringsflyt = AapFlyt(timerArbeidetRepository)

    private fun hentMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        return aapGateway.hentMeldeperioder(innloggetBruker, sak.rettighetsperiode)
            .map {
                /* TODO: behandlingsflyt burde bestemme hva meldevinduet er. */
                Meldeperiode(
                    meldeperioden = it,
                    meldevindu = Periode(it.tom.plusDays(1), it.tom.plusDays(8)),
                )
            }
    }

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        val senesteOpplysningsdato =
            timerArbeidetRepository.hentSenesteOpplysningsdato(innloggetBruker.ident, sak.referanse)
                ?: LocalDate.MIN

        val perioder = hentMeldeperioder(innloggetBruker)

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
        val perioder = hentMeldeperioder(innloggetBruker)

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
                aapGateway = GatewayProvider.provide(),
            )
        }
    }
}
