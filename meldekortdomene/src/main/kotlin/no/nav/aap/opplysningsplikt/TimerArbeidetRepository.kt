package no.nav.aap.opplysningsplikt

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.sak.FagsakReferanse


interface TimerArbeidetRepository: Repository {
    fun hentTimerArbeidet(
        ident: Ident,
        sak: FagsakReferanse,
        periode: Periode,
    ): List<TimerArbeidet>

    fun lagrTimerArbeidet(
        ident: Ident,
        opplysninger: List<TimerArbeidet>,
    )
}