package no.nav.aap.flyt

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.Ident
import no.nav.aap.arena.MeldekortId

interface UtfyllingRepository: Repository {
    fun last(ident: Ident, meldekortId: MeldekortId, utfyllingFlyt: UtfyllingFlyt): Utfylling?
    fun lagrUtfylling(utfylling: Utfylling)
}