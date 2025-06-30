package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.varsel.VarselService
import java.time.Clock

class KelvinMottakService(private val varselService: VarselService,
                          private val kelvinSakRepository: KelvinSakRepository) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakRepository = repositoryProvider.provide(),
        varselService = VarselService(repositoryProvider, gatewayProvider, clock),
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
        if (!Miljø.erProd()) {
            // NB! Hvis "!er prod" fjernes, vurder å fjerne det fra SendVarselJobbUtfører samtidig
            varselService.planleggFremtidigeVarsler(saksnummer)
        }
    }
}