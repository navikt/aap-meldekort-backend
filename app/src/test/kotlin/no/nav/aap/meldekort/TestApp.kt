package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.ArenaService
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.MeldekortSkjemaRepositoryPostgres
import no.nav.aap.meldekort.arena.MeldekortService
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start() // azurePort = 8081

    val dataSource = createTestcontainerPostgresDataSource()
    val meldekortService = MeldekortService(
        meldekortSkjemaRepository = MeldekortSkjemaRepositoryPostgres(dataSource),
        meldekortRepository = MeldekortRepositoryPostgres(dataSource),
    )

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        meldekortService = meldekortService,
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        arena = FakeArena,
        arenaService = ArenaService(FakeArena),
    )
}