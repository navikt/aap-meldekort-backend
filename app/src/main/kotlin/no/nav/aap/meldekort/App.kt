package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.SkjemaRepositoryPostgres
import no.nav.aap.meldekort.arena.UtfyllingRepositoryPostgres
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.saker.SakerGatewayImpl
import org.slf4j.LoggerFactory

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

    val dataSource = createPostgresDataSource(DbConfig.fromEnv(), prometheus)

    registerRepositories()
    registerGateways()

    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
        tokenxConfig = TokenxConfig(),
        azureConfig = AzureConfig(),
        dataSource = dataSource
    )
}

fun registerRepositories() {
    RepositoryRegistry
        .register<MeldekortRepositoryPostgres>()
        .register<SkjemaRepositoryPostgres>()
        .register<UtfyllingRepositoryPostgres>()
        .status()
}

private fun registerGateways() {
    GatewayRegistry
        .register<DokarkivGatewayImpl>()
        .register<SakerGatewayImpl>()
        .register<ArenaGatewayImpl>()
        .status()
}
