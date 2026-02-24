package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.utfylling.UtfyllingStegNavn.KVITTERING
import no.nav.aap.varsel.VarselService
import java.time.Clock
import java.time.Instant

class KelvinMottakService(
    private val varselService: VarselService,
    private val kelvinSakRepository: KelvinSakRepository,
    private val utfyllingRepository: UtfyllingRepository,
    private val aktivitetsInformasjonRepository: AktivitetsInformasjonRepository,
    private val clock: Clock
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakRepository = repositoryProvider.provide(),
        utfyllingRepository = repositoryProvider.provide(),
        aktivitetsInformasjonRepository = repositoryProvider.provide(),
        varselService = VarselService(repositoryProvider, gatewayProvider, clock),
        clock = clock
    )

    fun behandleMottatteMeldeperioder(
        saksnummer: Fagsaknummer,
        sakenGjelderFor: Periode,
        identer: List<Ident>,
        meldeperioder: List<Periode>,
        meldeplikt: List<Periode>,
        opplysningsbehov: List<Periode>,
        status: KelvinSakStatus?
    ) {
        kelvinSakRepository.upsertSak(
            saksnummer = saksnummer,
            sakenGjelderFor = sakenGjelderFor,
            identer = identer,
            meldeperioder = meldeperioder,
            meldeplikt = meldeplikt,
            opplysningsbehov = opplysningsbehov,
            status = status
        )
        varselService.planleggFremtidigeVarsler(saksnummer)
    }

    fun behandleMottatteAktivitetsInformasjon(
        ident: Ident,
        periode: Periode,
        harDuJobbet: Boolean,
        aktivitetsInformasjon: List<no.nav.aap.utfylling.AktivitetsInformasjon>
    ): UtfyllingReferanse {
        val sak = kelvinSakRepository.hentSak(ident, periode.fom)
            ?: throw IllegalStateException("finner ikke sak")

        val åpenUtfylling =
            utfyllingRepository.lastÅpenUtfylling(ident, periode)

        val utfyllingReferanse = åpenUtfylling?.referanse ?: UtfyllingReferanse.ny()

        val utfylling = Utfylling(
            referanse = utfyllingReferanse,
            periode = periode,
            fagsak = sak.referanse,
            ident = ident,
            flyt = åpenUtfylling?.flyt ?: UtfyllingFlytNavn.AAP_FLYT_V2,
            aktivtSteg = KVITTERING,
            svar = Svar(
                svarerDuSant = true, // Antar dette når bruker sender inn eget papir
                harDuJobbet = harDuJobbet,
                aktivitetsInformasjon = aktivitetsInformasjon,
                stemmerOpplysningene = true // Antar dette når bruker sender inn eget papir,
            ),
            opprettet = Instant.now(clock),
            sistEndret = Instant.now(clock),
            erDigitalisert = true
        )

        utfyllingRepository.lagrUtfylling(utfylling)
        aktivitetsInformasjonRepository.lagrAktivitetsInformasjon(
            ident = utfylling.ident,
            opplysninger = utfylling.svar.aktivitetsInformasjon.map {
                no.nav.aap.opplysningsplikt.AktivitetsInformasjon(
                    registreringstidspunkt = utfylling.sistEndret,
                    utfylling = utfylling.referanse,
                    fagsak = utfylling.fagsak,
                    dato = it.dato,
                    timerArbeidet = it.timer,
                    fravær = null
                )
            }
        )

        varselService.inaktiverVarslerForUtfylling(utfylling)
        return utfyllingReferanse
    }
}