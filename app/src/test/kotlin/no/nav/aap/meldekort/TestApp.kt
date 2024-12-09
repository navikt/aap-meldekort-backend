package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.Arena
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.MeldekortService
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start() // azurePort = 8081

    val dataSource = createTestcontainerPostgresDataSource()
    val meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    val meldekortService = MeldekortService(meldekortRepository)

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        meldekortService = meldekortService,
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        arena = object: Arena {
            override fun meldegrupper(innloggetBruker: InnloggetBruker): List<Arena.Meldegruppe> {
                TODO("Not yet implemented")
            }

            override fun meldekort(innloggetBruker: InnloggetBruker): Arena.Person? {
                TODO("Not yet implemented")
            }

            override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): Arena.Person {
                TODO("Not yet implemented")
            }

            override fun meldekortdetaljer(
                innloggetBruker: InnloggetBruker,
                meldekortId: Long
            ): Arena.Meldekortdetaljer {
                TODO("Not yet implemented")
            }

            override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long {
                TODO("Not yet implemented")
            }

            override fun sendInn(
                innloggetBruker: InnloggetBruker,
                request: Arena.MeldekortkontrollRequest
            ): Arena.MeldekortkontrollResponse {
                TODO("Not yet implemented")
            }

        }
    )
}