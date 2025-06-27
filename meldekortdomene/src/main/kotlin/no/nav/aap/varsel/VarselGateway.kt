package no.nav.aap.varsel

import no.nav.aap.Ident
import no.nav.aap.lookup.gateway.Gateway

interface VarselGateway: Gateway {
    fun sendVarsel(brukerId: Ident,
                   varsel: Varsel,
                   varselTekster: VarselTekster,
                   lenke: String)
    fun inaktiverVarsel(varsel: Varsel)
}