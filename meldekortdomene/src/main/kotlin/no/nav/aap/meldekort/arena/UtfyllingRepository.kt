package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

interface UtfyllingRepository {
    fun last(ident: Ident, meldekortId: Long, utfyllingFlyt: UtfyllingFlyt): Utfylling?
    fun lagrUtfylling(skjema: Utfylling)
}