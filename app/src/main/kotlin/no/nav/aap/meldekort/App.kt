package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
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

    val meldekortService = MeldekortService(meldekortRepository)
    startHttpServer(
        port = 8080,
        prometheus = prometheus,
        meldekortService = meldekortService,
        applikasjonsVersjon = ApplikasjonsVersjon.versjon,
    )
}
