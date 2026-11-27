package no.nav.aap.kallogg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class SlettGamleKallLoggerJobbUtfører private constructor(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val clock: Clock,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val mottattTilOgMed = clock.instant().minus(90, ChronoUnit.DAYS)
        while (slettPulje(mottattTilOgMed) == PULJESTØRRELSE) {
            // Hver pulje committes før neste starter.
        }
    }

    private fun slettPulje(mottattTilOgMed: Instant): Int = dataSource.transaction { connection ->
        repositoryRegistry.provider(connection).provide<KallLoggRepository>()
            .slettUtløpte(mottattTilOgMed, PULJESTØRRELSE)
    }

    companion object {
        private const val PULJESTØRRELSE = 1_000

        fun jobbKonstruktør(dataSource: DataSource, repositoryRegistry: RepositoryRegistry, clock: Clock): Jobb =
            object : Jobb {
                override fun type() = "batch.slettGamleKallLogger"
                override fun navn() = "Slett gamle kallogger"
                override fun beskrivelse() = "Sletter kallogger som ble mottatt for minst 90 dager siden."
                override fun cron() = CronExpression.createWithoutSeconds("15 0 * * *")

                override fun konstruer(connection: DBConnection): JobbUtfører =
                    SlettGamleKallLoggerJobbUtfører(dataSource, repositoryRegistry, clock)
            }
    }
}
