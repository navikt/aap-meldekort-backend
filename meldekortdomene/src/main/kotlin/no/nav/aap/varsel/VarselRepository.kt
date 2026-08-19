package no.nav.aap.varsel

import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.Fagsaknummer
import java.time.Clock

interface VarselRepository: Repository {
    fun hentVarsler(saksnummer: Fagsaknummer): List<Varsel>
    fun upsert(varsel: Varsel)
    fun slettPlanlagtVarsel(varselId: VarselId)
    fun hentVarslerForUtsending(clock: Clock) : List<Varsel>
}