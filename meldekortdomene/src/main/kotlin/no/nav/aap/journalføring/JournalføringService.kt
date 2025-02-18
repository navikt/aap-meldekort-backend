package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.arena.MeldekortId
import no.nav.aap.journalføring.motor.ArenaJournalføringJobbUtfører
import no.nav.aap.motor.FlytJobbRepository

class JournalføringService(
    private val flytJobbRepository: FlytJobbRepository
) {
    fun bestillJournalføring(ident: Ident, meldekortId: MeldekortId) {
        flytJobbRepository.leggTil(
            ArenaJournalføringJobbUtfører.jobbInput(
            ident, meldekortId
        ))
    }

}