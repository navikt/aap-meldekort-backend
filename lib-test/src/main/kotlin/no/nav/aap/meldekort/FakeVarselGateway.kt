package no.nav.aap.meldekort

import no.nav.aap.Ident
import no.nav.aap.varsel.Varsel
import no.nav.aap.varsel.VarselGateway
import no.nav.aap.varsel.VarselTekster
import org.slf4j.LoggerFactory

object FakeVarselGateway : VarselGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVarsel(
        brukerId: Ident,
        varsel: Varsel,
        varselTekster: VarselTekster,
        lenke: String
    ) {
        log.info("""
            Sender varsel:
            brukerId: $brukerId
            varsel: $varsel
            varselTekster: $varselTekster
            lenke: $lenke
        """.trimIndent())
    }

    override fun inaktiverVarsel(varsel: Varsel) {
        log.info("Inaktiver varsel: $varsel")
    }
}