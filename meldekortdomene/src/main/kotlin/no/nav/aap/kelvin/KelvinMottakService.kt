package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.sak.Fagsaknummer

class KelvinMottakService(private val kelvinSakRepository: KelvinSakRepository) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        kelvinSakRepository = repositoryProvider.provide(),
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
    }
}