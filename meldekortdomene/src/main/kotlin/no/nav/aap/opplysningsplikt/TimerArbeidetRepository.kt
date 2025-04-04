package no.nav.aap.opplysningsplikt

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.FagsakReferanse
import java.time.LocalDate


interface TimerArbeidetRepository: Repository {
    /** @return registrerte opplysininger, sorted på dato (ascending) */
    fun hentTimerArbeidet(
        ident: Ident,
        sak: FagsakReferanse,
        periode: Periode,
    ): List<TimerArbeidet>

    fun lagrTimerArbeidet(
        ident: Ident,
        opplysninger: List<TimerArbeidet>,
    )

    fun hentSenesteOpplysningsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate?
}