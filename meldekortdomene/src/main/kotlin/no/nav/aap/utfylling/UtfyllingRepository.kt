package no.nav.aap.utfylling

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.Ident
import no.nav.aap.Periode

interface UtfyllingRepository: Repository {
    fun lastÅpenUtfylling(ident: Ident, periode: Periode): Utfylling?
    fun lastAvsluttetUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling?
    fun lastUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling?
    fun lagrUtfylling(utfylling: Utfylling)
    fun slettUtkast(ident: Ident, utfyllingReferanse: UtfyllingReferanse)
}