package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.ArenaClientImpl
import no.nav.aap.meldekort.arena.ArenaService
import no.nav.aap.meldekort.arena.MeldekortRepositoryPostgres
import no.nav.aap.meldekort.arenaflyt.MeldekortService
import no.nav.aap.meldekort.arena.MeldekortSkjemaRepositoryPostgres
import org.slf4j.LoggerFactory

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

    val dataSource = createPostgresDataSource(DbConfig.fromEnv())

    val arena = ArenaClientImpl(
        meldekortserviceScope = requiredConfigForKey("meldekortservice.scope"),
        meldekortkontrollScope = requiredConfigForKey("meldekortkontroll.scope"),
        meldekortserviceUrl = requiredConfigForKey("meldekortservice.url"),
        meldekortkontrollUrl = requiredConfigForKey("meldekortkontroll.url"),
    )

    val meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    val arenaService = ArenaService(arena, meldekortRepository)

    val meldekortService = MeldekortService(
        meldekortSkjemaRepository = MeldekortSkjemaRepositoryPostgres(dataSource),
        meldekortRepository = meldekortRepository,
        arenaService = arenaService
    )

    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        meldekortService = meldekortService,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
        tokenxConfig = TokenxConfig(),
        arenaClient = arena,
        arenaService = arenaService,
    )
}
