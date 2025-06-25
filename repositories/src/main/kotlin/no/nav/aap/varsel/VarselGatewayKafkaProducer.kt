package no.nav.aap.varsel

import no.nav.aap.Ident
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder

object VarselGatewayKafkaProducer : VarselGateway {
    override fun sendVarsel(brukerId: Ident, varsel: Varsel, varselTekster: VarselTekster) {
        val melding = opprettKafkaJson(brukerId, varsel, varselTekster)
        TODO("Not yet implemented")
    }

    override fun inaktiverVarsel(varsel: Varsel) {
        val melding = VarselActionBuilder.inaktiver { varselId = varsel.varselId.id.toString() }
        TODO("Not yet implemented")
    }

    private fun opprettKafkaJson(
        brukerId: Ident,
        varsel: Varsel,
        varselTekster: VarselTekster
    ): String {
        return VarselActionBuilder.opprett {
            type = when (varsel.typeVarsel) {
                TypeVarsel.BESKJED -> Varseltype.Beskjed
                TypeVarsel.OPPGAVE -> Varseltype.Oppgave
            }
            varselId = varsel.varselId.id.toString()
            sensitivitet = Sensitivitet.High
            ident = brukerId.asString
            tekster += Tekst(
                spraakkode = "nb",
                tekst = varselTekster.nb,
                default = true
            )
            tekster += Tekst(
                spraakkode = "nn",
                tekst = varselTekster.nn,
                default = false
            )
            tekster += Tekst(
                spraakkode = "en",
                tekst = varselTekster.en,
                default = false
            )
            link = "https://www.nav.no" // TODO lenke til meldekort
            aktivFremTil = null // TODO aktuell med mindre vi inaktiverer selv
            eksternVarsling {
                preferertKanal = EksternKanal.SMS // TODO også for varseltype varsel?
            }
        }
    }
}