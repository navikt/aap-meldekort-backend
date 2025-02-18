package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaService
import no.nav.aap.arena.MeldekortService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.sak.FagsystemService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.fagsystemServiceFactory
import java.time.Instant
import java.time.LocalDate

class UtfyllingService(
    private val utfyllingRepository: UtfyllingRepository,
    private val fagsystemService: FagsystemService,
    private val utfyllingsflyter: Utfyllingsflyter,
) {

    fun startUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): Utfylling {
        val utfylling = eksisterendeUtfylling(innloggetBruker, periode)
        if (utfylling != null) {
            return utfylling
        }

        val utfyllingReferanse = UtfyllingReferanse.ny()
        fagsystemService.forberedVanligFlyt(innloggetBruker, periode, utfyllingReferanse)

        return nyUtfylling(
            utfyllingReferanse = utfyllingReferanse,
            ident = innloggetBruker.ident,
            periode = periode,
            flyt = fagsystemService.innsendingsflyt,
            svar = fagsystemService.tomtSvar(periode),
        )
    }

    fun startKorrigering(innloggetBruker: InnloggetBruker, periode: Periode): Utfylling {
        val utfylling = eksisterendeUtfylling(innloggetBruker, periode)
        if (utfylling != null) {
            return utfylling
        }

        val utfyllingReferanse = UtfyllingReferanse.ny()
        fagsystemService.forberedKorrigeringFlyt(innloggetBruker, periode, utfyllingReferanse)

        return nyUtfylling(
            utfyllingReferanse = utfyllingReferanse,
            ident = innloggetBruker.ident,
            periode = periode,
            flyt = fagsystemService.korrigeringsflyt,
            svar = fagsystemService.hentHistoriskeSvar(innloggetBruker, periode),
        )
    }

    private fun eksisterendeUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): Utfylling? {
        val utfylling = utfyllingRepository.lastÅpenUtfylling(innloggetBruker.ident, periode, utfyllingsflyter) ?: return null

        if (fagsystemService.utfyllingGyldig(utfylling)) {
            return utfylling
        }

        return null
    }


    private fun nyUtfylling(
        utfyllingReferanse: UtfyllingReferanse,
        ident: Ident,
        periode: Periode,
        flyt: UtfyllingFlyt,
        svar: Svar,
    ): Utfylling {
        val opprettet = Instant.now()
        val nyUtfylling = Utfylling(
            referanse = utfyllingReferanse,
            periode = periode,
            ident = ident,
            flyt = flyt,
            aktivtSteg = flyt.steg.first(),
            svar = svar,
            opprettet = opprettet,
            sistEndret = opprettet,
        )
        utfyllingRepository.lagrUtfylling(nyUtfylling)
        return nyUtfylling
    }


    fun hent(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse): Utfylling? {
        return utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse, utfyllingsflyter)
    }

    class UtfyllingResponse(
        val utfylling: Utfylling,
        val feil: Exception? = null,
    )

    fun nesteOgLagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse {
        val eksisterendeUtfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse, utfyllingsflyter)
            ?: TODO("kan skje hvis mellomlagring slettes")

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = eksisterendeUtfylling.flyt.stegForNavn(aktivtSteg),
            svar = svar,
            sistEndret = Instant.now(),
        )
        val utfall = UtfyllingFlytOrkestrator.kjør(innloggetBruker, utfylling)
        utfyllingRepository.lagrUtfylling(utfall.utfylling)
        return UtfyllingResponse(
            utfylling = utfall.utfylling,
            feil = utfall.feil,
        )
    }

    fun lagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingResponse {
        val eksisterendeUtfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse, utfyllingsflyter)
            ?: TODO("kan skje hvis mellomlagring slettes")

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = eksisterendeUtfylling.flyt.stegForNavn(aktivtSteg),
            svar = svar,
            sistEndret = Instant.now(),
        )

        utfyllingRepository.lagrUtfylling(utfylling)
        return UtfyllingResponse(utfylling)

    }

    fun slett(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse) {
        TODO("Not yet implemented")
    }

    companion object {
        fun konstruer(innloggetBruker: InnloggetBruker, connection: DBConnection): UtfyllingService {
            val sak = SakerService.konstruer().finnSak(innloggetBruker, LocalDate.now())
                ?: error("frontend skal ikke prøve å starte utfylling uten at sak eksisterer")

            val repositoryProvider = RepositoryProvider(connection)

            val arenaService = ArenaService(
                meldekortService = MeldekortService(
                    arenaGateway = GatewayProvider.provide(),
                    meldekortRepository = repositoryProvider.provide(),
                ),
                arenaGateway = GatewayProvider.provide(),
                sak = sak,
            )

            return UtfyllingService(
                utfyllingRepository = repositoryProvider.provide(),
                fagsystemService = fagsystemServiceFactory(connection, sak),
                utfyllingsflyter = Utfyllingsflyter(arenaService)
            )
        }
    }
}