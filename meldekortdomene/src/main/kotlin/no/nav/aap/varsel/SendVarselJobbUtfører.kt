package no.nav.aap.varsel

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import java.time.Clock

class SendVarselJobbUtfører(
    private val varselService: VarselService,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        varselService.sendPlanlagteVarsler()
    }

    companion object {
        private val jobbInfo = object : Jobb {
            override fun beskrivelse() = "Send varsler som er klare til utsending."
            override fun type() = "batch.sendVarsler"
            override fun navn() = "Send varsler"

            override fun cron() = CronExpression.createWithoutSeconds("0 2 * * *")

            override fun konstruer(connection: DBConnection): JobbUtfører {
                error("kun intern for jobb info")
            }
        }

        fun jobbKonstruktør(
            repositoryRegistry: RepositoryRegistry,
            clock: Clock
        ) = object : Jobb by jobbInfo {
            override fun konstruer(connection: DBConnection): JobbUtfører {
                val repositoryProvider = repositoryRegistry.provider(connection)

                return SendVarselJobbUtfører(VarselService(repositoryProvider, GatewayProvider, clock))
            }
        }
    }

}