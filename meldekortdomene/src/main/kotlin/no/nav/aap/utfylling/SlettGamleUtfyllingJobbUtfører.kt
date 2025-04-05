package no.nav.aap.utfylling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import java.time.Clock
import java.time.LocalDate

class SlettGamleUtfyllingJobbUtfører(
    private val utfyllingRepository: UtfyllingRepository,
    private val clock: Clock,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        utfyllingRepository.slettGamleUtkast(slettTilOgMed = LocalDate.now(clock).minusDays(31))
    }

    companion object {
        private val jobbInfo = object : Jobb {
            override fun beskrivelse() = """
            Slett gamle utfyllinger som ikke er sendt inn.
        """.trimIndent()
            override fun type() = "batch.slettGamleUtfyllinger"
            override fun navn() = "Slett gamle utfyllinger"

            override fun cron() = CronExpression.createWithoutSeconds("0 2 * * *")

            override fun konstruer(connection: DBConnection): JobbUtfører {
                error("kun intern for jobb info")
            }
        }

        fun jobbKonstruktør(repositoryRegistry: RepositoryRegistry, clock: Clock) = object: Jobb by jobbInfo {
            override fun konstruer(connection: DBConnection): JobbUtfører {
                val repositoryProvider = repositoryRegistry.provider(connection)
                return SlettGamleUtfyllingJobbUtfører(repositoryProvider.provide(), clock)
            }
        }
    }
}