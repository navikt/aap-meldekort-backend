package no.nav.aap.meldekort.arena

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.meldekort.Ident

interface UtfyllingRepository: Repository {
    fun last(ident: Ident, meldekortId: MeldekortId, utfyllingFlyt: UtfyllingFlyt): Utfylling?
    fun lagrUtfylling(utfylling: Utfylling)
}