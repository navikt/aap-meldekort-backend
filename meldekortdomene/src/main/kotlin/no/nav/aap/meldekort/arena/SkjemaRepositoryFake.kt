package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
class SkjemaRepositoryFake: SkjemaRepository {
    private val skjema: HashMap<Pair<Ident, Long>, Skjema> = HashMap()

    override fun last(ident: Ident, meldekortId: Long, skjemaFlyt: SkjemaFlyt): Skjema? {
        return skjema[ident to meldekortId]
    }

    override fun lagrSkjema(skjema: Skjema) {
        this.skjema[skjema.ident to skjema.meldekortId] = skjema
    }
}