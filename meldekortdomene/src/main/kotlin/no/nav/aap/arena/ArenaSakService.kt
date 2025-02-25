package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.SakService
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class ArenaSakService(
    private val meldekortService: MeldekortService,
    private val arenaGateway: ArenaGateway,
    override val sak: Sak,
) : SakService {
    override val innsendingsflyt = UtfyllingFlytNavn.ARENA_VANLIG_FLYT
    override val korrigeringsflyt = UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): SakService.VentendeOgNeste {
        val kommendeMeldekort = meldekortService.kommendeMeldekort(innloggetBruker)
            .orEmpty()
            .sortedBy { it.tidligsteInnsendingsdato }

        val klareForInnsending = kommendeMeldekort.filter { it.kanSendes }

        val neste = klareForInnsending.firstOrNull()
            ?: kommendeMeldekort.firstOrNull { !it.kanSendes }
        return SakService.VentendeOgNeste(
            ventende = klareForInnsending.map {
                Meldeperiode(
                    meldeperioden = it.periode,
                    meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX), /* TODO */
                )
            },
            neste = neste?.let {
                Meldeperiode(
                    meldeperioden = it.periode,
                    meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX), /* TODO */
                )
            },
        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val historiskMeldekort = meldekortService.historiskeMeldekort(innloggetBruker)
            .groupBy { it.periode }
            .values
            .map { it.maxBy { meldekort -> meldekort.beregningStatus.ordinal } }

        return historiskMeldekort.map {
            Meldeperiode(
                meldeperioden = it.periode,
                meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX),
            )
        }
    }

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): SakService.PeriodeDetaljer {
        val meldekort = meldekortService.gjeldeneHistoriskeMeldekort(innloggetBruker, periode)
            ?: error("meldekortet burde ha blitt listet opp, så er forventet at det eksisterer")

        /* TODO: finn timer arbeidet fra utfylling om det mangler. */
        val timerArbeidet = arenaGateway.meldekortdetaljer(innloggetBruker, meldekort.meldekortId)
            .timerArbeidet(meldekort.periode.fom)
            ?.map { TimerArbeidet(it.dato, it.timer) }
            ?: meldekort.periode.map { TimerArbeidet(it, null) }

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
        return Svar.tomt(periode)
    }

    override fun opplysningerForJournalpost(utfylling: Utfylling): SakService.OpplysningerForJournalpost {
        val uke1 = utfylling.periode.fom.get(uke)
        val uke2 = utfylling.periode.tom.get(uke)
        val fra = utfylling.periode.fom.format(dateFormatter)
        val til = utfylling.periode.tom.format(dateFormatter)
        val tittelsuffix = "for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av NAV"

        val tilleggsopplysning = mapOf<String, String>(
            // TODO:
//                "meldekortId" to skjema.meldekortId.toString(),
//                "kortKanSendesFra" to kanSendesFra.format(dateFormatter),
        )

        return when (utfylling.flyt) {
            UtfyllingFlytNavn.ARENA_VANLIG_FLYT ->
                SakService.OpplysningerForJournalpost(
                    tittel = "Meldekort $tittelsuffix",
                    brevkode = "NAV 00-10.02",
                    tilleggsopplysning = tilleggsopplysning,
                )

            UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT ->
                SakService.OpplysningerForJournalpost(
                    tittel = "Korrigert meldekort $tittelsuffix",
                    brevkode = "NAV 00-10.03",
                    tilleggsopplysning = tilleggsopplysning,
                )

            UtfyllingFlytNavn.AAP_FLYT ->
                error("arena service kan ikke utlede journalpost-opplysninger for utfylling ${utfylling.referanse} med fagsak ${utfylling.fagsak}")
        }
    }

    fun sendInnKorrigering(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    fun sendInnVanlig(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): ArenaSakService {
            val repositoryProvider = RepositoryProvider(connection)
            return ArenaSakService(
                meldekortService = MeldekortService(
                    arenaGateway = GatewayProvider.provide(),
                    meldekortRepository = repositoryProvider.provide(),
                ),
                arenaGateway = GatewayProvider.provide(),
                sak = sak,
            )
        }

        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY")
        private val uke = WeekFields.of(Locale.of("nb", "NO")).weekOfWeekBasedYear()
    }
}