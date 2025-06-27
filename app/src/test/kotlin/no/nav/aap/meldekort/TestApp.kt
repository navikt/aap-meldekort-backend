package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.kelvin.KelvinMottakService
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn.ARENA
import no.nav.aap.sak.FagsystemNavn.KELVIN
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

fun main() {
    FakeTokenX.port = 8081
    FakeServers.start()

    setupRegistries()
    GatewayRegistry.register<FakeVarselGateway>()

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
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val kelvinMottakService = KelvinMottakService(repositoryProvider, GatewayProvider, Clock.systemDefaultZone())
        val iDag = LocalDate.now()
        val sistMandag = generateSequence(iDag) { it.minusDays(1) }
            .first { it.dayOfWeek == DayOfWeek.MONDAY }

        kelvinMottakService.behandleMottatteMeldeperioder(
            saksnummer = Fagsaknummer("1015"),
            sakenGjelderFor = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)),
            identer = listOf(Ident("11111111111")),
            meldeperioder =
                lagPerioder(
                    sistMandag.minusDays(14) to sistMandag.minusDays(1),
                    sistMandag to sistMandag.plusDays(13),
                    sistMandag.plusDays(14) to sistMandag.plusDays(27),
                ),
            meldeplikt =
                lagPerioder(
                    sistMandag to sistMandag.plusDays(7),
                    sistMandag.plusDays(14) to sistMandag.plusDays(21),
                    sistMandag.plusDays(28) to sistMandag.plusDays(35),
                ),
            opplysningsbehov = listOf(
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)),
            ),
            status = KelvinSakStatus.LØPENDE,
        )
    }

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

private fun lagPerioder(vararg fomTom: Pair<LocalDate, LocalDate>): List<Periode> {
    return fomTom.map { (fom, tom) -> Periode(fom, tom) }
}