package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.DbConfig
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.arena.MeldekortRepositoryPostgres
import no.nav.aap.createPostgresDataSource
import no.nav.aap.journalføring.FakeDokgenGateway
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.opplysningsplikt.TimerArbeidetRepositoryPostgres
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

    main(
        dataSource = createPostgresDataSource(DbConfig.fromEnv(), prometheus),
        prometheusMeterRegistry = prometheus,
        applikasjonVersjon = ApplikasjonsVersjon.versjon
    )
}

fun main(
    dataSource: DataSource,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjonVersjon: String,
) {
    RepositoryRegistry
        .register<MeldekortRepositoryPostgres>()
        .register<UtfyllingRepositoryPostgres>()
        .register<TimerArbeidetRepositoryPostgres>()
        .status()

    GatewayRegistry
        .register<DokarkivGatewayImpl>()
        .register<AapGatewayImpl>()
        .register<ArenaGatewayImpl>()
        .register<FakeDokgenGateway>()
        .status()

    startHttpServer(
        port = 8080,
        prometheus = prometheusMeterRegistry,
        applikasjonsVersjon = applikasjonVersjon,
        tokenxConfig = TokenxConfig(),
        azureConfig = AzureConfig(),
        dataSource = dataSource
    )
}
