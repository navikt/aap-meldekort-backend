package no.nav.aap.varsel

import no.nav.aap.Ident
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.INAKTIVER_VARSEL

class InaktiverVarselSteg(
    private val varselService: VarselService,
) : UtfyllingSteg {
    override val navn = INAKTIVER_VARSEL
    override fun utførEffekt(ident: Ident, utfylling: Utfylling) {
        varselService.inaktiverVarslerForUtfylling(utfylling)
    }
}