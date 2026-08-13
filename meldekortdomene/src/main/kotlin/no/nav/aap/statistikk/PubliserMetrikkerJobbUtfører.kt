package no.nav.aap.statistikk

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import java.util.concurrent.atomic.AtomicLong

class PubliserMetrikkerJobbUtfører(
    private val statistikkRepository: StatistikkRepository,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val statistikk = statistikkRepository.hentStatistikk()

        mottatteMeldekortTotalt.set(statistikk.mottatteMeldekortTotalt)
        mottatteMeldekortIDag.set(statistikk.mottatteMeldekortIDag)
        varslerSendtIDag.set(statistikk.varslerSendtIDag)
        varslerInaktivertIDag.set(statistikk.varslerInaktivertIDag)
        varslerPlanlagt.set(statistikk.varslerPlanlagt)
    }

    companion object {
        private val mottatteMeldekortTotalt = AtomicLong(0)
        private val mottatteMeldekortIDag = AtomicLong(0)
        private val varslerSendtIDag = AtomicLong(0)
        private val varslerInaktivertIDag = AtomicLong(0)
        private val varslerPlanlagt = AtomicLong(0)

        private val jobbInfo = object : Jobb {
            override fun beskrivelse() = "Publiserer metrikker for meldekort og varsler"
            override fun type() = "batch.publiserMetrikker"
            override fun navn() = "Publiser metrikker"
            override fun cron() = CronExpression.createWithoutSeconds("*/15 * * * *")
            override fun konstruer(connection: DBConnection): JobbUtfører =
                error("kun intern for jobb info")
        }

        fun registrerMetrics(prometheus: PrometheusMeterRegistry) {
            Gauge.builder("meldekort_mottatt_totalt", mottatteMeldekortTotalt) { it.get().toDouble() }
                .description("Totalt antall innsendte meldekort")
                .register(prometheus)
            Gauge.builder("meldekort_mottatt_siste_24_timer", mottatteMeldekortIDag) { it.get().toDouble() }
                .description("Antall meldekort mottatt siste 24 timer")
                .register(prometheus)
            Gauge.builder("varsler_sendt_i_dag", varslerSendtIDag) { it.get().toDouble() }
                .description("Antall varsler sendt i dag")
                .register(prometheus)
            Gauge.builder("varsler_inaktivert_i_dag", varslerInaktivertIDag) { it.get().toDouble() }
                .description("Antall varsler inaktivert i dag")
                .register(prometheus)
            Gauge.builder("varsler_planlagt", varslerPlanlagt) { it.get().toDouble() }
                .description("Antall planlagte varsler i kø")
                .register(prometheus)
        }

        fun jobbKonstruktør(
            repositoryRegistry: RepositoryRegistry,
            prometheus: PrometheusMeterRegistry,
        ) = object : Jobb by jobbInfo {
            init {
                registrerMetrics(prometheus)
            }

            override fun konstruer(connection: DBConnection): JobbUtfører {
                val repositoryProvider = repositoryRegistry.provider(connection)
                return PubliserMetrikkerJobbUtfører(repositoryProvider.provide())
            }
        }
    }
}
