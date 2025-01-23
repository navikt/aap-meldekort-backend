package no.nav.aap.meldekort.journalføring.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class JournalførJobbUtfører: JobbUtfører {
    private val log = LoggerFactory.getLogger(JournalførJobbUtfører::class.java)

    override fun utfør(input: JobbInput) {
        log.info("nå utfører vi en jobb")
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "journalføringsjobb"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return JournalførJobbUtfører()
        }

        override fun navn(): String {
            return "journalføring"
        }

        override fun type(): String {
            return "type"
        }

    }
}
