package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.saker.SakerGatewayImpl
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start()

    main(
        dataSource = createTestcontainerPostgresDataSource(prometheus),
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        applikasjonVersjon = "TestApp"
    )
}