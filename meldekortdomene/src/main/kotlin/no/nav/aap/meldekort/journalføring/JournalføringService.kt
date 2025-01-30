package no.nav.aap.meldekort.journalføring

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.arena.MeldekortId
import no.nav.aap.meldekort.journalføring.motor.ArenaJournalføringJobbUtfører
import no.nav.aap.motor.FlytJobbRepository

class JournalføringService(
    private val flytJobbRepository: FlytJobbRepository
) {
    fun journalfør(ident: Ident, meldekortId: MeldekortId) {
        flytJobbRepository.leggTil(ArenaJournalføringJobbUtfører.jobbInput(
            ident, meldekortId
        ))
    }

}