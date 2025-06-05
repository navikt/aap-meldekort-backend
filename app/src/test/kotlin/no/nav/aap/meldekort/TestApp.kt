package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn.ARENA
import no.nav.aap.sak.FagsystemNavn.KELVIN
import no.nav.aap.sak.Sak
import java.time.Clock
import java.time.LocalDate

fun main() {
    FakeTokenX.port = 8081
    FakeServers().start()

    setupRegistries()

    val idag = LocalDate.now()
    FakeAapApi.upsert(
        Ident("1".repeat(11)),
        FakeAapApi.FakeSak(
            referanse = FagsakReferanse(KELVIN, Fagsaknummer("1015")),
            rettighetsperiode = Periode(idag.minusDays(100), idag.plusDays(20)),
        )
    )

    FakeAapApi.upsert(
        Ident("2".repeat(11)),
        FakeAapApi.FakeSak(
            referanse = FagsakReferanse(ARENA, Fagsaknummer("")),
            rettighetsperiode = Periode(idag.minusDays(100), idag.plusDays(20)),
        )
    )


    val dataSource = createTestcontainerPostgresDataSource(prometheus)

    dataSource.transaction { connection ->
        val kelvinSakRepository = KelvinSakRepositoryPostgres(connection)
        kelvinSakRepository.upsertSak(
            saksnummer = Fagsaknummer("111111"),
            sakenGjelderFor = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)),
            identer = listOf(Ident("11111111111")),
            meldeperioder =
                listOf(
                    "2025-02-10" to "2025-02-23",
                    "2025-02-24" to "2025-03-09",
                    "2025-03-10" to "2025-03-23",
                    "2025-03-24" to "2025-04-06",
                ).map { (fom, tom) -> Periode(LocalDate.parse(fom), LocalDate.parse(tom)) },
            meldeplikt = listOf(),
            opplysningsbehov = listOf(
                Periode(LocalDate.of(2025, 2, 13), LocalDate.of(2025, 4, 8)),
            ),
//            status = KelvinSakStatus.LØPENDE,
            status = KelvinSakStatus.UTREDES
        )
    }

    setupRegistries()

    startHttpServer(
        port = 8080,
        prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        applikasjonsVersjon = "TestApp",
        tokenxConfig = TokenxConfig(),
        azureConfig = AzureConfig(),
        dataSource = dataSource,
        repositoryRegistry = postgresRepositoryRegistry,
        clock = Clock.systemDefaultZone(),
    )
}