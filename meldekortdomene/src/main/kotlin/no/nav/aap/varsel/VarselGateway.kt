package no.nav.aap.varsel

import no.nav.aap.lookup.gateway.Gateway

interface VarselGateway: Gateway {
    fun sendVarsel(varsel: Varsel)
    fun inaktiverVarsel(varsel: Varsel)
}