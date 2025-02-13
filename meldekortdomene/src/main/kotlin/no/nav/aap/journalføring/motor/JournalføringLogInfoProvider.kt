package no.nav.aap.journalføring.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon

object JournalføringLogInfoProvider: JobbLogInfoProvider {
    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {
        return LogInformasjon(mapOf("hei" to "du"))
    }
}