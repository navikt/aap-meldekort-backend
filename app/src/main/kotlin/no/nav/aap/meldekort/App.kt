package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.arena.ArenaClient
import no.nav.aap.meldekort.arena.ArenaService
import no.nav.aap.meldekort.arena.MeldekortRepositoryFake
import no.nav.aap.meldekort.arena.MeldekortService
import org.slf4j.LoggerFactory

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }

//    val dataSource = createPostgresDatasource(DbConfig.fromEnv())
//    val meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    val meldekortRepository = MeldekortRepositoryFake()

    val arena = ArenaClient(
        meldekortserviceScope = requiredConfigForKey("meldekortservice.scope"),
        meldekortkontrollScope = requiredConfigForKey("meldekortkontroll.scope"),
        meldekortserviceUrl = requiredConfigForKey("meldekortservice.url"),
        meldekortkontrollUrl = requiredConfigForKey("meldekortkontroll.url"),
    )

    val meldekortService = MeldekortService(meldekortRepository)
    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        meldekortService = meldekortService,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
        tokenxConfig = TokenxConfig(),
        arena = arena,
        arenaService = ArenaService(arena),
    )
}
