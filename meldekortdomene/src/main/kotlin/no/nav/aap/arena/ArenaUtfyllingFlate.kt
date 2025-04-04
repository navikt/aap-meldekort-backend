package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.sak.Sak
import no.nav.aap.sak.SakService
import no.nav.aap.sak.SakerService
import no.nav.aap.sak.sakServiceFactory
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingFlyt
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ArenaUtfyllingFlate(
    private val utfyllingRepository: UtfyllingRepository,
    private val sakService: SakService,
    private val flytProvider: (UtfyllingFlytNavn) -> UtfyllingFlyt,
) : UtfyllingFlate {

    private fun antallMeldeperioderUtenOpplysninger(innloggetBruker: InnloggetBruker): Int {
        return sakService.ventendeOgNesteMeldeperioder(innloggetBruker).ventende.size
    }

    override fun startUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): UtfyllingFlate.StartUtfyllingResponse {
        val utfylling = eksisterendeUtfylling(innloggetBruker, periode) ?: run {
            val utfyllingReferanse = UtfyllingReferanse.ny()
            sakService.forberedVanligFlyt(innloggetBruker, periode, utfyllingReferanse)

            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = innloggetBruker.ident,
                periode = periode,
                flyt = sakService.innsendingsflyt,
                svar = sakService.tomtSvar(periode),
                sak = sakService.sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun startKorrigering(innloggetBruker: InnloggetBruker, periode: Periode): UtfyllingFlate.StartUtfyllingResponse {
        val utfylling = eksisterendeUtfylling(innloggetBruker, periode) ?: run {
            val utfyllingReferanse = UtfyllingReferanse.ny()
            sakService.forberedKorrigeringFlyt(innloggetBruker, periode, utfyllingReferanse)

            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = innloggetBruker.ident,
                periode = periode,
                flyt = sakService.korrigeringsflyt,
                svar = sakService.hentHistoriskeSvar(innloggetBruker, periode),
                sak = sakService.sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    private fun utledMetadata(innloggetBruker: InnloggetBruker, utfylling: Utfylling): UtfyllingFlate.Metadata {
        val tidligsteInnsendingstidspunkt = utfylling.periode.tom.plusDays(1).atStartOfDay()
        val fristForInnsending = utfylling.periode.tom.plusDays(9).atTime(23, 59)
        val kanSendesInn = tidligsteInnsendingstidspunkt <= LocalDateTime.now(ZoneId.of("Europe/Oslo"))

        return UtfyllingFlate.Metadata(
            referanse = utfylling.referanse,
            periode = utfylling.periode,
            antallUbesvarteMeldeperioder = antallMeldeperioderUtenOpplysninger(innloggetBruker),
            tidligsteInnsendingstidspunkt = tidligsteInnsendingstidspunkt,
            fristForInnsending = fristForInnsending,
            kanSendesInn = kanSendesInn,
        )
    }


    private fun eksisterendeUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): Utfylling? {
        val utfylling = utfyllingRepository.lastÅpenUtfylling(innloggetBruker.ident, periode) ?: return null

        if (sakService.utfyllingGyldig(utfylling)) {
            return utfylling
        }

        return null
    }


    private fun nyUtfylling(
        utfyllingReferanse: UtfyllingReferanse,
        ident: Ident,
        periode: Periode,
        flyt: UtfyllingFlytNavn,
        svar: Svar,
        sak: Sak,
    ): Utfylling {
        val opprettet = Instant.now()
        val nyUtfylling = Utfylling(
            referanse = utfyllingReferanse,
            periode = periode,
            fagsak = sak.referanse,
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



    override fun hentUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse): UtfyllingFlate.UtfyllingResponse? {
        val utfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse)
            ?: return null

        val tidligsteInnsendingstidspunkt = utfylling.periode.tom.plusDays(1).atStartOfDay()
        val fristForInnsending = utfylling.periode.tom.plusDays(9).atTime(23, 59)
        val kanSendesInn = tidligsteInnsendingstidspunkt <= LocalDateTime.now(ZoneId.of("Europe/Oslo"))

        return UtfyllingFlate.UtfyllingResponse(
            metadata = UtfyllingFlate.Metadata(
                referanse = utfylling.referanse,
                periode = utfylling.periode,
                antallUbesvarteMeldeperioder = antallMeldeperioderUtenOpplysninger(innloggetBruker),
                tidligsteInnsendingstidspunkt = tidligsteInnsendingstidspunkt,
                fristForInnsending = fristForInnsending,
                kanSendesInn = kanSendesInn,
            ),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun nesteOgLagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        val eksisterendeUtfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse)
            ?: TODO("kan skje hvis mellomlagring slettes")

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = aktivtSteg,
            svar = svar,
            sistEndret = Instant.now(),
        )
        val utfall = flytProvider(utfylling.flyt).kjør(innloggetBruker, utfylling)
        utfyllingRepository.lagrUtfylling(utfall.utfylling)
        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfall.utfylling,
            feil = utfall.feil?.javaClass?.canonicalName,
        )
    }

    override fun lagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        val eksisterendeUtfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse)
            ?: TODO("kan skje hvis mellomlagring slettes")

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = aktivtSteg,
            svar = svar,
            sistEndret = Instant.now(),
        )

        utfyllingRepository.lagrUtfylling(utfylling)
        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun slettUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse) {
        val utfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse) ?: return
        require(!utfylling.erAvsluttet)
        utfyllingRepository.slettUtkast(innloggetBruker.ident, utfyllingReferanse)
    }

    companion object {
        fun konstruer(innloggetBruker: InnloggetBruker, connection: DBConnection): UtfyllingFlate {
            val sak = SakerService.konstruer(connection).finnSak(innloggetBruker, LocalDate.now())
                ?: error("frontend skal ikke prøve å starte utfylling uten at sak eksisterer")

            val repositoryProvider = RepositoryProvider(connection)

            return ArenaUtfyllingFlate(
                utfyllingRepository = repositoryProvider.provide(),
                sakService = sakServiceFactory(connection, sak),
                flytProvider = { flytNavn -> UtfyllingFlyt.konstruer(connection, sak, flytNavn) }
            )
        }
    }
}
