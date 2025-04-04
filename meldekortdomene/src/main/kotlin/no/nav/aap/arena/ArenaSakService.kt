package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.utfylling.Utfylling

class ArenaSakService(
//    private val meldekortService: MeldekortService,
//    private val arenaGateway: ArenaGateway,
//    val sak: Sak,
) {
//    val innsendingsflyt = UtfyllingFlytNavn.ARENA_VANLIG_FLYT
//    val korrigeringsflyt = UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT
//
//    fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): VentendeOgNeste {
//        val kommendeMeldekort = meldekortService.kommendeMeldekort(innloggetBruker)
//            .orEmpty()
//            .sortedBy { it.tidligsteInnsendingsdato }
//
//        val klareForInnsending = kommendeMeldekort.filter { it.kanSendes }
//
//        val neste = klareForInnsending.firstOrNull()
//            ?: kommendeMeldekort.firstOrNull { !it.kanSendes }
//        return SakService.VentendeOgNeste(
//            ventende = klareForInnsending.map {
//                Meldeperiode(
//                    meldeperioden = it.periode,
//                    meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX), /* TODO */
//                )
//            },
//            neste = neste?.let {
//                Meldeperiode(
//                    meldeperioden = it.periode,
//                    meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX), /* TODO */
//                )
//            },
//        )
//    }
//
//    fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
//        val historiskMeldekort = meldekortService.historiskeMeldekort(innloggetBruker)
//            .groupBy { it.periode }
//            .values
//            .map { it.maxBy { meldekort -> meldekort.beregningStatus.ordinal } }
//
//        return historiskMeldekort.map {
//            Meldeperiode(
//                meldeperioden = it.periode,
//                meldevindu = Periode(it.tidligsteInnsendingsdato, LocalDate.MAX),
//            )
//        }
//    }
//
//    fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): MeldeperiodeFlate.PeriodeDetaljer {
//        val meldekort = meldekortService.gjeldeneHistoriskeMeldekort(innloggetBruker, periode)
//            ?: error("meldekortet burde ha blitt listet opp, så er forventet at det eksisterer")
//
//        /* TODO: finn timer arbeidet fra utfylling om det mangler. */
//        val timerArbeidet = arenaGateway.meldekortdetaljer(innloggetBruker, meldekort.meldekortId)
//            .timerArbeidet(meldekort.periode.fom)
//            ?.map { TimerArbeidet(it.dato, it.timer) }
//            ?: meldekort.periode.map { TimerArbeidet(it, null) }
//
//        return MeldeperiodeFlate.PeriodeDetaljer(
//            periode = periode,
//            svar = Svar(
//                svarerDuSant = true, /* TODO */
//                harDuJobbet = true, /* TODO */
//                timerArbeidet = timerArbeidet,
//                stemmerOpplysningene = true, /* TODO */
//            )
//        )
//    }
//
//    /* TODO: implementer denne */
//    fun totaltAntallTimerArbeidet(periodeDetaljer: MeldeperiodeFlate.PeriodeDetaljer): Double {
//        return 0.0
//    }
//
//    fun forberedVanligFlyt(
//        innloggetBruker: InnloggetBruker,
//        periode: Periode,
//        utfyllingReferanse: UtfyllingReferanse
//    ) {
//    }
//
//    fun forberedKorrigeringFlyt(
//        innloggetBruker: InnloggetBruker,
//        periode: Periode,
//        utfyllingReferanse: UtfyllingReferanse
//    ) {
//    }
//
//    fun hentHistoriskeSvar(innloggetBruker: InnloggetBruker, periode: Periode): Svar {
//        return Svar.tomt(periode)
//    }
//
//    fun opplysningerForJournalpost(utfylling: Utfylling): SakService.OpplysningerForJournalpost {
//
//        val tilleggsopplysning = mapOf<String, String>(
//            // TODO:
////                "meldekortId" to skjema.meldekortId.toString(),
////                "kortKanSendesFra" to kanSendesFra.format(dateFormatter),
//        )
//
//        val journalførPåSak = DokarkivGateway.Sak(
//            sakstype = FAGSAK,
//            fagsaksystem = DokarkivGateway.FagsaksSystem.AO01,
//            fagsakId = sak.referanse.nummer.asString,
//        )
//
//        return when (utfylling.flyt) {
//            UtfyllingFlytNavn.ARENA_VANLIG_FLYT ->
//                SakService.OpplysningerForJournalpost(
//                    tittel = "Meldekort $tittelsuffix",
//                    brevkode = "NAV 00-10.02",
//                    tilleggsopplysning = tilleggsopplysning,
//                    ferdigstill = true,
//                    journalførPåSak = journalførPåSak,
//                )
//
//            UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT ->
//                SakService.OpplysningerForJournalpost(
//                    tittel = "Korrigert meldekort $tittelsuffix",
//                    brevkode = "NAV 00-10.03",
//                    tilleggsopplysning = tilleggsopplysning,
//                    ferdigstill = true,
//                    journalførPåSak = journalførPåSak,
//                )
//
//            UtfyllingFlytNavn.AAP_FLYT, UtfyllingFlytNavn.AAP_KORRIGERING_FLYT ->
//                error("arena service kan ikke utlede journalpost-opplysninger for utfylling ${utfylling.referanse} med fagsak ${utfylling.fagsak}")
//        }
//    }
//
    fun sendInnKorrigering(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    fun sendInnVanlig(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }
//
//    companion object {
//        fun konstruer(connection: DBConnection, sak: Sak): ArenaSakService {
//            val repositoryProvider = RepositoryProvider(connection)
//            return ArenaSakService(
//                meldekortService = MeldekortService(
//                    arenaGateway = GatewayProvider.provide(),
//                    meldekortRepository = repositoryProvider.provide(),
//                ),
//                arenaGateway = GatewayProvider.provide(),
//                sak = sak,
//            )
//        }
//
//    }
}