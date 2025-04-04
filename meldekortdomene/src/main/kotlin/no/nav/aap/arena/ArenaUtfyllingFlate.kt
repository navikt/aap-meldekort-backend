package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingStegNavn

class ArenaUtfyllingFlate(): UtfyllingFlate {

    constructor(repositoryProvider: RepositoryProvider): this()

    override fun startUtfylling(innloggetBruker: InnloggetBruker, periode: Periode): UtfyllingFlate.StartUtfyllingResponse {
        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = null,
            utfylling = null,
            feil = "Utfylling av arenakort er ikke støttet"
        )
    }

    override fun startKorrigering(innloggetBruker: InnloggetBruker, periode: Periode): UtfyllingFlate.StartUtfyllingResponse {
        return UtfyllingFlate.StartUtfyllingResponse(
            metadata = null,
            utfylling = null,
            feil = "Utfylling av arenakort er ikke støttet"
        )
    }

    override fun hentUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse): UtfyllingFlate.UtfyllingResponse? {
        return null
    }

    override fun nesteOgLagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        error("utfylling av arena-meldekort ikke støttet")
    }

    override fun lagre(
        innloggetBruker: InnloggetBruker,
        utfyllingReferanse: UtfyllingReferanse,
        aktivtSteg: UtfyllingStegNavn,
        svar: Svar,
    ): UtfyllingFlate.UtfyllingResponse {
        error("utfylling av arena-meldekort ikke støttet")
    }

    override fun slettUtfylling(innloggetBruker: InnloggetBruker, utfyllingReferanse: UtfyllingReferanse) {
        error("utfylling av arena-meldekoert er ikke støttet")
    }
}
