package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingFlyt
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class KelvinUtfyllingFlate(
    private val utfyllingRepository: UtfyllingRepository,
    private val kelvinSakRepository: KelvinSakRepository,
    private val sakService: KelvinSakService,
    private val flytProvider: (UtfyllingFlytNavn) -> UtfyllingFlyt,
) : UtfyllingFlate {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        utfyllingRepository = repositoryProvider.provide(),
        kelvinSakRepository = repositoryProvider.provide(),
        sakService = KelvinSakService(repositoryProvider, gatewayProvider, clock),
        flytProvider = { flytNavn -> UtfyllingFlyt.konstruer(repositoryProvider, gatewayProvider, flytNavn) }
    )


    override fun startUtfylling(
        innloggetBruker: InnloggetBruker,
        periode: Periode
    ): UtfyllingFlate.StartUtfyllingResponse {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, periode.fom)
            ?: return UtfyllingFlate.StartUtfyllingResponse(
                metadata = null,
                utfylling = null,
                feil = "finner ikke sak",
            )

        val utfylling = eksisterendeUtfylling(innloggetBruker, periode) ?: run {
            val utfyllingReferanse = UtfyllingReferanse.ny()

            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = innloggetBruker.ident,
                periode = periode,
                flyt = UtfyllingFlytNavn.AAP_FLYT,
                svar = Svar.tomt(periode),
                sak = sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun startKorrigering(
        innloggetBruker: InnloggetBruker,
        periode: Periode
    ): UtfyllingFlate.StartUtfyllingResponse {
        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, periode.fom)
            ?: return UtfyllingFlate.StartUtfyllingResponse(
                metadata = null,
                utfylling = null,
                feil = "finner ikke sak",
            )

        val utfylling = eksisterendeUtfylling(innloggetBruker, periode) ?: run {
            val utfyllingReferanse = UtfyllingReferanse.ny()

            val timerArbeidet = sakService.registrerteTimerArbeidet(innloggetBruker.ident, sak.referanse, periode)
            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = innloggetBruker.ident,
                periode = periode,
                flyt = UtfyllingFlytNavn.AAP_KORRIGERING_FLYT,
                svar = Svar(
                    svarerDuSant = null,
                    harDuJobbet = timerArbeidet.any { (it.timer ?: 0.0) > 0.0 },
                    timerArbeidet = timerArbeidet,
                    stemmerOpplysningene = null,
                ),
                sak = sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    private fun utledMetadata(innloggetBruker: InnloggetBruker, utfylling: Utfylling, brukerHarVedtakIKelvin: Boolean? = null, brukerHarSakUnderBehandling: Boolean? = null): UtfyllingFlate.Metadata {
        val tidligsteInnsendingstidspunkt = utfylling.periode.tom.plusDays(1).atStartOfDay()
        val fristForInnsending = utfylling.periode.tom.plusDays(8).atTime(23, 59)
//        val fristForInnsending = sakService.finnTidspunktForOpplysningsbehov(innloggetBruker.ident, utfylling.fagsak)
        val kanSendesInn = tidligsteInnsendingstidspunkt <= LocalDateTime.now(ZoneId.of("Europe/Oslo"))

        return UtfyllingFlate.Metadata(
            referanse = utfylling.referanse,
            periode = utfylling.periode,
            antallUbesvarteMeldeperioder = sakService.antallUbesvarteMeldeperioder(
                innloggetBruker.ident,
                utfylling.fagsak
            ),
            tidligsteInnsendingstidspunkt = tidligsteInnsendingstidspunkt,
            fristForInnsending = fristForInnsending,
            kanSendesInn = kanSendesInn,
            brukerHarVedtakIKelvin = brukerHarVedtakIKelvin,
            brukerHarSakUnderBehandling = brukerHarSakUnderBehandling,
        )
    }


    private fun eksisterendeUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): Utfylling? {
        /* TODO: noen sjekk på gyldighet? Utløpt? */
        return utfyllingRepository.lastÅpenUtfylling(innloggetBruker.ident, periode)
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

    override fun hentUtfylling(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse
    ): UtfyllingFlate.UtfyllingResponse? {
        val utfylling = utfyllingRepository.lastUtfylling(innloggetBruker.ident, utfyllingReferanse)
            ?: return null

        val sak = kelvinSakRepository.hentSak(innloggetBruker.ident, utfylling.periode.fom)
        val brukerHarVedtakIKelvin = sak?.erLøpende()
        val brukerHarSakUnderBehandling = sak?.erUnderBehandling()

        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(innloggetBruker, utfylling, brukerHarVedtakIKelvin, brukerHarSakUnderBehandling),
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
}
