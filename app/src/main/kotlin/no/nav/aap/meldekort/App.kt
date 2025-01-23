package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.meldekort.arena.ArenaClientImpl
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arena.SkjemaRepositoryPostgres
import no.nav.aap.meldekort.arena.UtfyllingRepositoryPostgres
import org.slf4j.LoggerFactory

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

    val dataSource = createPostgresDataSource(DbConfig.fromEnv(), prometheus)

    val arenaClient = ArenaClientImpl(
        meldekortserviceScope = requiredConfigForKey("meldekortservice.scope"),
        meldekortkontrollScope = requiredConfigForKey("meldekortkontroll.scope"),
        meldekortserviceUrl = requiredConfigForKey("meldekortservice.url"),
        meldekortkontrollUrl = requiredConfigForKey("meldekortkontroll.url"),
    )

    registerRepositories()

    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
        tokenxConfig = TokenxConfig(),
        arenaClient = arenaClient,
        dataSource = dataSource
    )
}

private fun registerRepositories() {
    RepositoryRegistry
        .register<MeldekortRepositoryPostgres>()
        .register<SkjemaRepositoryPostgres>()
        .register<UtfyllingRepositoryPostgres>()
        .status()
}
