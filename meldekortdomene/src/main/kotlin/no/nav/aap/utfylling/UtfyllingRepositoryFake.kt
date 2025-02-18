package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.arena.MeldekortId

/* TODO: Flytt til test-mappen når serveren ikke lenger bruker fake versjonen. */
//class UtfyllingRepositoryFake: UtfyllingRepository {
//    private val skjema: HashMap<Pair<Ident, MeldekortId>, Utfylling> = HashMap()
//
//    override fun last(ident: Ident, meldekortId: MeldekortId, utfyllingFlytOrkestrator: UtfyllingFlytOrkestrator): Utfylling? {
//        return skjema[ident to meldekortId]
//    }
//
//    override fun lagrUtfylling(skjema: Utfylling) {
//        this.skjema[skjema.skjema.ident to skjema.skjema.meldekortId] = skjema
//    }
//}