package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingFlyt
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingFlytNavn.AAP_FLYT
import no.nav.aap.utfylling.UtfyllingFlytNavn.AAP_FLYT_V2
import no.nav.aap.utfylling.UtfyllingFlytNavn.AAP_KORRIGERING_FLYT
import no.nav.aap.utfylling.UtfyllingFlytNavn.AAP_KORRIGERING_FLYT_V2
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

class KelvinUtfyllingFlate(
    private val utfyllingRepository: UtfyllingRepository,
    private val kelvinSakRepository: KelvinSakRepository,
    private val sakService: KelvinSakService,
    private val flytProvider: (UtfyllingFlytNavn) -> UtfyllingFlyt,
    private val clock: Clock
) : UtfyllingFlate {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        utfyllingRepository = repositoryProvider.provide(),
        kelvinSakRepository = repositoryProvider.provide(),
        sakService = KelvinSakService(repositoryProvider, clock),
        flytProvider = { flytNavn -> UtfyllingFlyt.konstruer(repositoryProvider, gatewayProvider, flytNavn, clock) },
        clock = clock,
    )


    override fun startUtfylling(
        ident: Ident,
        periode: Periode
    ): UtfyllingFlate.StartUtfyllingResponse {
        val sak = kelvinSakRepository.hentSak(ident, periode.fom)
            ?: return UtfyllingFlate.StartUtfyllingResponse(
                metadata = null,
                utfylling = null,
                feil = "finner ikke sak",
            )

        val utfylling = eksisterendeÅpenUtfylling(ident, periode) ?: run {
            val utfyllingReferanse = UtfyllingReferanse.ny()

            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = ident,
                periode = periode,
                flyt = run {
                    if (Miljø.erProd()) {
                        AAP_FLYT
                    } else {
                        AAP_FLYT_V2
                    }
                },
                svar = Svar.tomt(periode),
                sak = sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(ident, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun startKorrigering(
        ident: Ident,
        periode: Periode
    ): UtfyllingFlate.StartUtfyllingResponse {
        val sak = kelvinSakRepository.hentSak(ident, periode.fom)
            ?: return UtfyllingFlate.StartUtfyllingResponse(
                metadata = null,
                utfylling = null,
                feil = "finner ikke sak",
            )

        val utfylling = eksisterendeÅpenUtfylling(ident, periode) ?: run {
            val eksisterendeAvsluttetUtfylling = eksisterendeUtfylling(ident, periode)
            val utfyllingReferanse = UtfyllingReferanse.ny()

            val aktivitetsInformasjon = sakService.registrerteAktivitetsInformasjon(ident, sak.referanse, periode)
            nyUtfylling(
                utfyllingReferanse = utfyllingReferanse,
                ident = ident,
                periode = periode,
                flyt = run {
                    if (Miljø.erProd()) {
                        AAP_KORRIGERING_FLYT
                    } else {
                        AAP_KORRIGERING_FLYT_V2
                    }
                },
                svar = Svar(
                    svarerDuSant = eksisterendeAvsluttetUtfylling?.svar?.svarerDuSant,
                    harDuJobbet = aktivitetsInformasjon.any { (it.timer ?: 0.0) > 0.0 },
                    aktivitetsInformasjon = aktivitetsInformasjon,
                    stemmerOpplysningene = eksisterendeAvsluttetUtfylling?.svar?.stemmerOpplysningene,
                    harDuHattAvtalteAktiviteter = eksisterendeAvsluttetUtfylling?.svar?.harDuHattAvtalteAktiviteter,
                    harDuHattFravær = eksisterendeAvsluttetUtfylling?.svar?.harDuHattFravær,
                ),
                sak = sak,
            )
        }

        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = utledMetadata(ident, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    private fun utledMetadata(
        ident: Ident,
        utfylling: Utfylling,
        brukerHarVedtakIKelvin: Boolean? = null,
        brukerHarSakUnderBehandling: Boolean? = null,
    ): UtfyllingFlate.Metadata {

        val startPåNesteMeldeperiode = utfylling.periode.tom.plusDays(1)
        val tidligsteInnsendingstidspunkt =
            tidligsteInnsendingstidspunktMeldedag(startPåNesteMeldeperiode).atStartOfDay()

        val fristForInnsending = sakService.finnMeldepliktfristForPeriode(utfylling.fagsak, utfylling.periode)
        val periodeHarHattMeldeplikt = fristForInnsending != null

        val kanSendesInn = tidligsteInnsendingstidspunkt <= LocalDateTime.now(clock)

        return UtfyllingFlate.Metadata(
            referanse = utfylling.referanse,
            periode = utfylling.periode,
            antallUbesvarteMeldeperioder = sakService.antallUbesvarteMeldeperioder(
                ident,
                utfylling.fagsak
            ),
            tidligsteInnsendingstidspunkt = tidligsteInnsendingstidspunkt,
            fristForInnsending = fristForInnsending,
            kanSendesInn = kanSendesInn,
            brukerHarVedtakIKelvin = brukerHarVedtakIKelvin,
            brukerHarSakUnderBehandling = brukerHarSakUnderBehandling,
            visFrist = periodeHarHattMeldeplikt,
            flytNavn = utfylling.flyt
        )
    }


    private fun eksisterendeÅpenUtfylling(ident: Ident, periode: Periode): Utfylling? {
        /* TODO: noen sjekk på gyldighet? Utløpt? */
        return utfyllingRepository.lastÅpenUtfylling(ident, periode)
    }

    private fun eksisterendeUtfylling(ident: Ident, periode: Periode): Utfylling? {
        return utfyllingRepository.lastUtfylling(ident, periode)
    }


    private fun nyUtfylling(
        utfyllingReferanse: UtfyllingReferanse,
        ident: Ident,
        periode: Periode,
        flyt: UtfyllingFlytNavn,
        svar: Svar,
        sak: Sak,
    ): Utfylling {
        val opprettet = Instant.now(clock)
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
        utfyllingRepository.lagreUtfylling(nyUtfylling)
        return nyUtfylling
    }

    override fun hentUtfylling(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse
    ): UtfyllingFlate.UtfyllingResponse? {
        val utfylling = utfyllingRepository.lastUtfylling(ident, utfyllingReferanse)
            ?: return null

        val sak = kelvinSakRepository.hentSak(ident, utfylling.periode.fom)
        val brukerHarVedtakIKelvin = sak?.erLøpende()
        val brukerHarSakUnderBehandling = sak?.erUnderBehandling()

        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(ident, utfylling, brukerHarVedtakIKelvin, brukerHarSakUnderBehandling),
            utfylling = utfylling,
            feil = null,
        )
    }

    override fun nesteOgLagre(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        val eksisterendeUtfylling = lastUtfyllingForÅEndre(ident, utfyllingReferanse)

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = aktivtSteg,
            svar = svar,
            sistEndret = Instant.now(clock),
        )
        val utfall = flytProvider(utfylling.flyt).kjør(ident, utfylling)
        utfyllingRepository.lagreUtfylling(utfall.utfylling)
        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(ident, utfylling),
            utfylling = utfall.utfylling,
            feil = utfall.feil?.javaClass?.canonicalName,
        )
    }

    override fun lagre(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        val eksisterendeUtfylling = lastUtfyllingForÅEndre(ident, utfyllingReferanse)

        val utfylling = eksisterendeUtfylling.copy(
            aktivtSteg = aktivtSteg,
            svar = svar,
            sistEndret = Instant.now(clock),
        )

        utfyllingRepository.lagreUtfylling(utfylling)
        return UtfyllingFlate.UtfyllingResponse(
            metadata = utledMetadata(ident, utfylling),
            utfylling = utfylling,
            feil = null,
        )
    }

    private fun lastUtfyllingForÅEndre(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling {
        val eksisterendeUtfylling = utfyllingRepository.lastUtfylling(ident, utfyllingReferanse)

        if (eksisterendeUtfylling == null) {
            throw VerdiIkkeFunnetException("Utfyllingen er allerede slettet.")
        } else if (eksisterendeUtfylling.erAvsluttet) {
            throw UgyldigForespørselException("Kan ikke endre utfylling som er avsluttet.")
        }

        return eksisterendeUtfylling
    }

    override fun slettUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse) {
        val utfylling = utfyllingRepository.lastUtfylling(ident, utfyllingReferanse) ?: return
        if (utfylling.erAvsluttet) {
            throw UgyldigForespørselException("Kan ikke slette utfylling som er avsluttet.")
        }
        utfyllingRepository.slettUtkast(ident, utfyllingReferanse)
    }
}
