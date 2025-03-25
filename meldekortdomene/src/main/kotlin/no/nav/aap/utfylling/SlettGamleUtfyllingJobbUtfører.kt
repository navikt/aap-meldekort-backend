package no.nav.aap.utfylling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import java.time.LocalDate

class SlettGamleUtfyllingJobbUtfører(
    private val utfyllingRepository: UtfyllingRepository,
): JobbUtfører {
    override fun utfør(input: JobbInput) {
        utfyllingRepository.slettGamleUtkast(slettTilOgMed = LocalDate.now().minusDays(31))
    }

    companion object: Jobb {
        override fun beskrivelse() = """
            Slett gamle utfyllinger som ikke er sendt inn.
        """.trimIndent()

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            return SlettGamleUtfyllingJobbUtfører(repositoryProvider.provide())
        }

        override fun type() = "batch.slettGamleUtfyllinger"
        override fun navn() = "Slett gamle utfyllinger"

        override fun cron() = CronExpression.createWithoutSeconds("0 2 * * *")
    }
}