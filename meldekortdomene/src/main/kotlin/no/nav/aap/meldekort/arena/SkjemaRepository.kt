package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

interface SkjemaRepository {
    fun lastSkjema(ident: Ident, meldekortId: Long, skjemaFlyt: SkjemaFlyt): Skjema?
    fun lagrSkjema(skjema: Skjema): Skjema
}