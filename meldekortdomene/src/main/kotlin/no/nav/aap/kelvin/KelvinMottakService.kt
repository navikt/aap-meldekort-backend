package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
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
    private val timerArbeidetRepository: TimerArbeidetRepository,
    private val clock: Clock
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakRepository = repositoryProvider.provide(),
        utfyllingRepository = repositoryProvider.provide(),
        timerArbeidetRepository = repositoryProvider.provide(),
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

    fun behandleMottatteTimerArbeidet(
        ident: Ident,
        periode: Periode,
        harDuJobbet: Boolean,
        timerArbeidet: List<no.nav.aap.utfylling.TimerArbeidet>
    ) {
        val sak = kelvinSakRepository.hentSak(ident, periode.fom)
            ?: throw IllegalStateException("finner ikke sak")

        val utfyllingReferanse =
            utfyllingRepository.lastÅpenUtfylling(ident, periode)?.referanse ?: UtfyllingReferanse.ny()

        val utfylling = Utfylling(
            referanse = utfyllingReferanse,
            periode = periode,
            fagsak = sak.referanse,
            ident = ident,
            flyt = UtfyllingFlytNavn.AAP_FLYT,
            aktivtSteg = KVITTERING,
            svar = Svar(
                svarerDuSant = true, // Antar dette når bruker sender inn eget papir
                harDuJobbet = harDuJobbet,
                timerArbeidet = timerArbeidet,
                stemmerOpplysningene = true // Antar dette når bruker sender inn eget papir,
            ),
            opprettet = Instant.now(clock),
            sistEndret = Instant.now(clock),
        )

        utfyllingRepository.lagrUtfylling(utfylling)
        timerArbeidetRepository.lagrTimerArbeidet(
            ident = utfylling.ident,
            opplysninger = utfylling.svar.timerArbeidet.map {
                no.nav.aap.opplysningsplikt.TimerArbeidet(
                    registreringstidspunkt = utfylling.sistEndret,
                    utfylling = utfylling.referanse,
                    fagsak = utfylling.fagsak,
                    dato = it.dato,
                    timerArbeidet = it.timer,
                )
            }
        )

        varselService.inaktiverVarslerForUtfylling(utfylling)
    }
}