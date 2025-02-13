package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.journalføring.JournalføringService
import no.nav.aap.motor.FlytJobbRepository

class ArenaSkjemaFlate private constructor(
    private val meldekortService: MeldekortService,
    private val utfyllingService: UtfyllingService,
    private val skjemaService: SkjemaService,
    private val arenaClient: ArenaClient
) {
    companion object {
        fun konstruer(connection: DBConnection, arenaClient: ArenaClient): ArenaSkjemaFlate {
            val repositoryProvider = RepositoryProvider(connection)
            val meldekortService = MeldekortService(
                arenaClient = arenaClient,
                meldekortRepository = repositoryProvider.provide(MeldekortRepository::class)
            )
            val skjemaService = SkjemaService(
                meldekortService = meldekortService,
                skjemaRepository = repositoryProvider.provide(SkjemaRepository::class),
                journalføringService = JournalføringService(FlytJobbRepository(connection))
            )

            return ArenaSkjemaFlate(
                meldekortService = meldekortService,
                utfyllingService = UtfyllingService(
                    utfyllingRepository = repositoryProvider.provide(UtfyllingRepository::class),
                    meldekortService = meldekortService,
                    skjemaService = skjemaService
                ),
                skjemaService = skjemaService,
                arenaClient = arenaClient
            )
        }
    }

    fun korrigerMeldekort(
        innloggetBruker: InnloggetBruker,
        originalMeldekortId: MeldekortId,
        timerArbeidet: List<TimerArbeidet>
    ) {
        skjemaService.sendInnKorrigering(innloggetBruker, originalMeldekortId, timerArbeidet)
    }

    fun kommendeMeldekort(innloggetBruker: InnloggetBruker): List<KommendeMeldekort> {
        return requireNotNull(meldekortService.kommendeMeldekort(innloggetBruker))
    }

    data class HistoriskMeldekortDetaljer(
        val meldekort: HistoriskMeldekort,
        val timerArbeidet: List<TimerArbeidet>?
    )

    fun historiskeMeldekortDetaljer(
        innloggetBruker: InnloggetBruker,
        meldeperiode: Periode
    ): List<HistoriskMeldekortDetaljer> {
        val meldekort = meldekortService.historiskeMeldekort(innloggetBruker).filter { it.periode == meldeperiode }

        return meldekort
            .map {
                HistoriskMeldekortDetaljer(
                    meldekort = it.copy(
                        mottattIArena = it.mottattIArena ?: skjemaService.finnSkjema(
                            innloggetBruker.ident,
                            it.meldekortId
                        )?.sendtInn?.toLocalDate(),
                    ),
                    timerArbeidet = skjemaService.timerArbeidet(innloggetBruker, it.meldekortId)
                        ?: arenaClient.meldekortdetaljer(innloggetBruker, it.meldekortId)
                            .timerArbeidet(it.periode.fom)
                )
            }
            .sortedByDescending { it.meldekort.periode }
    }

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker): List<HistoriskMeldekort> {
        return meldekortService.historiskeMeldekort(innloggetBruker)
            .groupBy { it.periode }
            .values
            .map {
                it.maxBy { meldekort -> meldekort.beregningStatus.ordinal }
            }
    }

    class UtfyllingResponse(
        val utfylling: Utfylling,
        val meldekort: Meldekort,
        val feil: ArenaInnsendingFeiletException? = null,
    )

    fun hentEllerOpprettUtfylling(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): UtfyllingResponse {
        val utfylling = utfyllingService.hentEllerStartUtfylling(meldekortId, innloggetBruker)
        val meldekort = meldekortService.hentLokaltMeldekort(innloggetBruker.ident, meldekortId)!!
        return UtfyllingResponse(utfylling, meldekort)
    }

    fun gåTilNesteSteg(
        innloggetBruker: InnloggetBruker,
        meldekortId: MeldekortId,
        fraSteg: StegNavn,
        nyPayload: InnsendingPayload
    ): UtfyllingResponse {
        val utfylling = utfyllingService.hentUtfylling(
            ident = innloggetBruker.ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(fraSteg)
            ?.nyPayload(nyPayload)
            ?: throw Error() /* todo: 404 not found */

        val meldekort = meldekortService.hentLokaltMeldekort(innloggetBruker.ident, meldekortId)!!

        utfylling.validerUtkast()

        try {
            return UtfyllingResponse(
                utfylling = utfyllingService.lagreOgNeste(
                    innloggetBruker = innloggetBruker,
                    utfylling = utfylling,
                ),
                meldekort = meldekort,
            )
        } catch (e: ArenaInnsendingFeiletException) {
            return UtfyllingResponse(
                utfylling = utfylling,
                meldekort = meldekort,
                feil = e,
            )
        }
    }

    fun lagreSteg(
        ident: Ident,
        meldekortId: MeldekortId,
        nyPayload: InnsendingPayload,
        settSteg: StegNavn
    ): UtfyllingResponse {
        val utfylling = utfyllingService.hentUtfylling(
            ident = ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(settSteg)
            ?.nyPayload(nyPayload)
            ?: throw Error() /* todo: 404 not found */

        utfylling.validerUtkast()
        val meldekort = meldekortService.hentLokaltMeldekort(ident, meldekortId)!!
        return UtfyllingResponse(
            utfylling = utfyllingService.lagre(utfylling),
            meldekort = meldekort
        )
    }
}