package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.Fagsaknummer
import java.time.LocalDate

interface UtfyllingRepository: Repository {
    fun lastÅpenUtfylling(ident: Ident, periode: Periode): Utfylling?
    fun lastAvsluttetUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling?
    fun lastUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling?
    fun hentUtfyllinger(saksnummer: Fagsaknummer): List<Utfylling>
    fun lagrUtfylling(utfylling: Utfylling)
    fun slettUtkast(ident: Ident, utfyllingReferanse: UtfyllingReferanse)
    fun slettGamleUtkast(slettTilOgMed: LocalDate)
}