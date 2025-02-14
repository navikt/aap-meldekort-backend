package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.meldekort.journalføring.JoarkGatewayImpl
import no.nav.aap.meldekort.saker.SakerGatewayImpl
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start() // azurePort = 8081

    GatewayRegistry
        .register<JoarkGatewayImpl>()
        .register<SakerGatewayImpl>()
        .register<ArenaGatewayImpl>()
        .status()

    val dataSource = createTestcontainerPostgresDataSource(prometheus)

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        dataSource = dataSource,
    )
}