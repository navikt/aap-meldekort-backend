package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.saker.SakerGatewayImpl
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start()

    GatewayRegistry
        .register<DokarkivGatewayImpl>()
        .register<SakerGatewayImpl>()
        .register<ArenaGatewayImpl>()
        .status()

    val dataSource = createTestcontainerPostgresDataSource(prometheus)

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        azureConfig = AzureConfig(),
        dataSource = dataSource,
    )
}