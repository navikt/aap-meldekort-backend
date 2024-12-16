package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.ArenaService
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.MeldekortSkjemaRepositoryPostgres
import no.nav.aap.meldekort.arenaflyt.MeldekortService
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start() // azurePort = 8081

    val dataSource = createTestcontainerPostgresDataSource()
    val meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    val arenaService = ArenaService(FakeArenaClient, meldekortRepository)
    val meldekortService = MeldekortService(
        meldekortSkjemaRepository = MeldekortSkjemaRepositoryPostgres(dataSource),
        meldekortRepository = meldekortRepository,
        arenaService,
    )

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        meldekortService = meldekortService,
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        arenaClient = FakeArenaClient,
        arenaService = arenaService,
    )
}