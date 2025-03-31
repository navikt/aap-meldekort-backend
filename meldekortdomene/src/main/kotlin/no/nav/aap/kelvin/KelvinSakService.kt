package no.nav.aap.kelvin

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.SakService
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate

class KelvinSakService(
    override val sak: Sak,
    private val kelvinSakRepository: KelvinSakRepository,
    private val timerArbeidetRepository: TimerArbeidetRepository,
) : SakService {
    override val innsendingsflyt = UtfyllingFlytNavn.AAP_FLYT
    override val korrigeringsflyt = UtfyllingFlytNavn.AAP_KORRIGERING_FLYT

    private fun hentMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val opplysningsbehov = kelvinSakRepository.hentOpplysningsbehov(innloggetBruker.ident, sak.referanse.nummer)
        val meldeperioder = kelvinSakRepository.hentMeldeperioder(innloggetBruker.ident, sak.referanse.nummer)

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

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker, dagensDato: LocalDate?): SakService.VentendeOgNeste {
        val senesteOpplysningsdato =
            timerArbeidetRepository.hentSenesteOpplysningsdato(innloggetBruker.ident, sak.referanse)
                ?: LocalDate.MIN

        val perioder = hentMeldeperioder(innloggetBruker)

        val meldeperioderUtenInnsending =
            perioder.dropWhile { it.meldeperioden.tom <= senesteOpplysningsdato }
        val meldeperioderUtenInnsendingSomKanSendesInn = meldeperioderUtenInnsending
            .takeWhile { it.meldevindu.tom <= (dagensDato ?: LocalDate.now()) }

        return SakService.VentendeOgNeste(
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

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): SakService.PeriodeDetaljer {
        val timerArbeidet = registrerteTimerArbeidet(innloggetBruker, periode)
        return SakService.PeriodeDetaljer(
            periode = periode,
            svar = Svar(
                svarerDuSant = true, /* TODO */
                harDuJobbet = true, /* TODO */
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = true, /* TODO */
            )
        )
    }

    override fun totaltAntallTimerArbeidet(periodeDetaljer: SakService.PeriodeDetaljer): Double {
        return periodeDetaljer.svar.timerArbeidet.sumOf { it.timer ?: 0.0 }
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
        val timerArbeidet = registrerteTimerArbeidet(innloggetBruker, periode)
        return Svar(
            svarerDuSant = null,
            harDuJobbet = timerArbeidet.any { (it.timer ?: 0.0) > 0.0 },
            timerArbeidet = timerArbeidet,
            stemmerOpplysningene = null,
        )
    }

    override fun opplysningerForJournalpost(utfylling: Utfylling): SakService.OpplysningerForJournalpost {
       return SakService.OpplysningerForJournalpost(
           tittel = "Meldekort",
           brevkode = "NAV 00-10.02" /* TODO: avklar brevkode */,
           tilleggsopplysning = mapOf(),

           /* postmottak håndterer dette */
           ferdigstill = false,
           journalførPåSak = null,
       )
    }

    private fun registrerteTimerArbeidet(
        innloggetBruker: InnloggetBruker,
        periode: Periode
    ): List<TimerArbeidet> {
        val registrerteOpplysninger =
            timerArbeidetRepository.hentTimerArbeidet(innloggetBruker.ident, sak.referanse, periode)
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

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): KelvinSakService {
            val repositoryProvider = RepositoryProvider(connection)
            return KelvinSakService(
                sak = sak,
                timerArbeidetRepository = repositoryProvider.provide(),
                kelvinSakRepository = repositoryProvider.provide(),
            )
        }
    }
}
