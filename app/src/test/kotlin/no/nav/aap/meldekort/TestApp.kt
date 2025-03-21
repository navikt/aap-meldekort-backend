package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.meldekort.test.FakeServers

fun main() {
    FakeServers.start()

    setupRegistries()

    main(
        dataSource = createTestcontainerPostgresDataSource(prometheus),
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        applikasjonVersjon = "TestApp"
    )
}