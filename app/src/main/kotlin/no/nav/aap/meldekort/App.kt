package no.nav.aap.meldekort

import no.nav.aap.DbConfig
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.createPostgresDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.arena.ArenaGatewayImpl
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.journalføring.PdfgenGatewayImpl
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.postgresRepositoryRegistry
import org.slf4j.LoggerFactory

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

    setupRegistries()

    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
        tokenxConfig = TokenxConfig(),
        azureConfig = AzureConfig(),
        dataSource = createPostgresDataSource(DbConfig.fromEnv(), prometheus),
        repositoryRegistry = postgresRepositoryRegistry
    )
}

fun setupRegistries() {

    GatewayRegistry
        .register<DokarkivGatewayImpl>()
        .register<AapGatewayImpl>()
        .register<ArenaGatewayImpl>()
        .register<PdfgenGatewayImpl>()
        .status()
}
