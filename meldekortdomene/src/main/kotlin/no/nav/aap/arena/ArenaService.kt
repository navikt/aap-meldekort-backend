package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.ArenaKorrigeringFlyt
import no.nav.aap.utfylling.ArenaVanligFlyt
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate

class ArenaService(
    private val meldekortService: MeldekortService,
    private val arenaGateway: ArenaGateway,
    override val sak: Sak,
    timerArbeidetRepository: TimerArbeidetRepository,
) : FagsystemService {
    override val innsendingsflyt = ArenaVanligFlyt(this, timerArbeidetRepository)
    override val korrigeringsflyt = ArenaKorrigeringFlyt(this, timerArbeidetRepository)

    override fun ventendeOgNesteMeldeperioder(innloggetBruker: InnloggetBruker): FagsystemService.VentendeOgNeste {
        val kommendeMeldekort = meldekortService.kommendeMeldekort(innloggetBruker)
            .orEmpty()
            .sortedBy { it.tidligsteInnsendingsdato }

        val klareForInnsending = kommendeMeldekort.filter { it.kanSendes }

        val neste = klareForInnsending.firstOrNull()
            ?: kommendeMeldekort.firstOrNull { !it.kanSendes }
        return FagsystemService.VentendeOgNeste(
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

    override fun detaljer(innloggetBruker: InnloggetBruker, periode: Periode): FagsystemService.PeriodeDetaljer {
        val meldekort = meldekortService.gjeldeneHistoriskeMeldekort(innloggetBruker, periode)
            ?: error("meldekortet burde ha blitt listet opp, så er forventet at det eksisterer")

        /* TODO: finn timer arbeidet fra utfylling om det mangler. */
        val timerArbeidet = arenaGateway.meldekortdetaljer(innloggetBruker, meldekort.meldekortId)
            .timerArbeidet(meldekort.periode.fom)
            ?.map { TimerArbeidet(it.dato, it.timer) }
            ?: meldekort.periode.map { TimerArbeidet(it, null) }

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

    fun sendInnKorrigering(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    fun sendInnVanlig(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): ArenaService {
            return ArenaService(
                meldekortService = MeldekortService(
                    arenaGateway = GatewayProvider.provide(),
                    meldekortRepository = RepositoryProvider(connection).provide(),
                ),
                arenaGateway = GatewayProvider.provide(),
                sak = sak,
                timerArbeidetRepository = RepositoryProvider(connection).provide(),
            )
        }
    }
}