package no.nav.aap.varsel

import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.Fagsaknummer

interface VarselRepository: Repository {
    fun hentVarsler(fagsaknummer: Fagsaknummer): List<Varsel>
    fun upsert(varsel: Varsel)
    fun slettPlanlagteVarsler(fagsaknummer: Fagsaknummer, typeVarselOm: TypeVarselOm)
    fun hentVarslerForUtsending() : List<Varsel>
}