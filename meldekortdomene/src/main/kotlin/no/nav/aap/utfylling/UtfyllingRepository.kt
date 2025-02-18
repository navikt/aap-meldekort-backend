package no.nav.aap.utfylling

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.Ident
import no.nav.aap.Periode

interface UtfyllingRepository: Repository {
    fun lastÅpenUtfylling(ident: Ident, periode: Periode, utfyllingsflyter: Utfyllingsflyter): Utfylling?
    fun lastUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse, utfyllingsflyter: Utfyllingsflyter): Utfylling?
    fun lagrUtfylling(utfylling: Utfylling)
}