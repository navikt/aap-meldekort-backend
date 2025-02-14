package no.nav.aap.skjema

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.Ident
import no.nav.aap.arena.MeldekortId

data class SkjemaId(val asLong: Long)

interface SkjemaRepository: Repository {
    fun last(ident: Ident, meldekortId: MeldekortId): Skjema?
    fun lastInnsendtSkjema(ident: Ident, meldekortId: MeldekortId): Skjema?
    fun lagrSkjema(skjema: Skjema): SkjemaId
}