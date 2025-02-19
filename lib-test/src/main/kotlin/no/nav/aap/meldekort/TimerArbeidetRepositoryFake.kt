package no.nav.aap.meldekort

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.opplysningsplikt.TimerArbeidet
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsakReferanse
import java.time.LocalDate

class TimerArbeidetRepositoryFake: TimerArbeidetRepository   {
    override fun hentTimerArbeidet(ident: Ident, sak: FagsakReferanse, periode: Periode): List<TimerArbeidet> {
        return listOf()
    }

    override fun lagrTimerArbeidet(ident: Ident, opplysninger: List<TimerArbeidet>) {
    }

    override fun hentSisteInnsendingsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate? {
        return null
    }
}