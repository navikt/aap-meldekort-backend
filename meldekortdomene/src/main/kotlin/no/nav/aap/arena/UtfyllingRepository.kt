package no.nav.aap.arena

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.Ident

interface UtfyllingRepository: Repository {
    fun last(ident: Ident, meldekortId: MeldekortId, utfyllingFlyt: UtfyllingFlyt): Utfylling?
    fun lagrUtfylling(utfylling: Utfylling)
}