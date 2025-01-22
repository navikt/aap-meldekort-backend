package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

data class SkjemaId(val asLong: Long)

interface SkjemaRepository {
    fun last(ident: Ident, meldekortId: Long): Skjema?
    fun lagrSkjema(skjema: Skjema): SkjemaId
}