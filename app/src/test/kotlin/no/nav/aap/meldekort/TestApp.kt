package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.ArenaSkjemaFlate
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.MeldekortService
import no.nav.aap.meldekort.arena.SkjemaRepositoryPostgres
import no.nav.aap.meldekort.arena.SkjemaService
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start() // azurePort = 8081

    val dataSource = createTestcontainerPostgresDataSource()

    val meldekortService = MeldekortService(
        arenaClient = FakeArenaClient,
        meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    )

    val skjemaService = SkjemaService(
        skjemaRepository = SkjemaRepositoryPostgres(dataSource),
        meldekortService = meldekortService,
    )

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        arenaSkjemaFlate = ArenaSkjemaFlate(
            meldekortService = meldekortService,
            skjemaService = skjemaService,
        ),
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        arenaClient = FakeArenaClient,
    )
}