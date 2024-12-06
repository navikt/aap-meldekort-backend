package no.nav.aap.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.meldekort.arena.MeldekortRepositoryFake
import no.nav.aap.meldekort.arena.MeldekortService
import org.slf4j.LoggerFactory
import java.net.URI

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
        /* TODO: her må nok felleskomponenten oppdateres ... */
        azureConfig = AzureConfig(
            tokenEndpoint = URI(requiredConfigForKey("token.x.token.endpoint")),
            clientId = requiredConfigForKey("token.x.client.id"),
            clientSecret = "",
            jwksUri = requiredConfigForKey("token.x.jwks.uri"),
            issuer = requiredConfigForKey("token.x.issuer"),
        ),
    )
}
