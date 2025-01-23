package no.nav.aap.meldekort.journalføring

import no.nav.aap.meldekort.arena.SkjemaRepository
import no.nav.aap.meldekort.journalføring.motor.JournalførJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput

class JournalføringService(
    private val flytJobbRepository: FlytJobbRepository
) {
    fun journalfør() {
        flytJobbRepository.leggTil(JobbInput(
            JournalførJobbUtfører
        ))
    }

}