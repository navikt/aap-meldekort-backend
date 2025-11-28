package no.nav.aap.meldekort

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinMottakService
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.journalføring.PdfgenGatewayImpl
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn.ARENA
import no.nav.aap.sak.FagsystemNavn.KELVIN
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

fun main() {
    FakeTokenX.port = 8081
    FakeServers.start()

    val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
    kafkaContainer.start()
    System.setProperty("KAFKA_BROKERS", kafkaContainer.bootstrapServers)
    System.setProperty("aap.meldekort.lenke", "https://aap-meldekort.ansatt.dev.nav.no/aap/meldekort")
    System.setProperty("brukervarsel.topic", "brukervarsel-topic")

    GatewayRegistry
        .register<DokarkivGatewayImpl>()
        .register<AapGatewayImpl>()
        .register<PdfgenGatewayImpl>()
        .register<VarselGatewayKafkaProducerTestcontainers>()
        .status()

    val idag = LocalDate.of(2025, 12, 29)
    val rettighetsPeriode = Periode(LocalDate.of(2025, 12, 13), idag.plusDays(110))
    FakeAapApi.upsert(
        Ident("1".repeat(11)),
        FakeAapApi.FakeSak(
            referanse = FagsakReferanse(KELVIN, Fagsaknummer("1015")),
            rettighetsperiode = rettighetsPeriode,
        )
    )

    FakeAapApi.upsert(
        Ident("2".repeat(11)),
        FakeAapApi.FakeSak(
            referanse = FagsakReferanse(ARENA, Fagsaknummer("")),
            rettighetsperiode = rettighetsPeriode,
        )
    )


    val dataSource = createTestcontainerPostgresDataSource(prometheus)

    dataSource.transaction { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val kelvinMottakService = KelvinMottakService(
            repositoryProvider,
            GatewayProvider,
            Clock.fixed(idag.atTime(10, 10).atZone(ZoneId.of("Europe/Oslo")).toInstant(), ZoneId.of("Europe/Oslo"))
        )
        val sistMandag = generateSequence(rettighetsPeriode.fom) { it.minusDays(1) }
            .first { it.dayOfWeek == DayOfWeek.MONDAY }

        kelvinMottakService.behandleMottatteMeldeperioder(
            saksnummer = Fagsaknummer("1015"),
            sakenGjelderFor = rettighetsPeriode,
            identer = listOf(Ident("11111111111")),
            meldeperioder =
                lagPerioder(
                    sistMandag to sistMandag.plusDays(13),
                    sistMandag.plusDays(14) to sistMandag.plusDays(27),
                ),
            meldeplikt =
                lagPerioder(
                    sistMandag.plusDays(14) to sistMandag.plusDays(21),
                    sistMandag.plusDays(28) to sistMandag.plusDays(35),
                ),
            opplysningsbehov = listOf(
                rettighetsPeriode,
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
        clock = Clock.fixed(idag.atTime(10, 10).atZone(ZoneId.of("Europe/Oslo")).toInstant(), ZoneId.of("Europe/Oslo")),
    )
}

private fun lagPerioder(vararg fomTom: Pair<LocalDate, LocalDate>): List<Periode> {
    return fomTom.map { (fom, tom) -> Periode(fom, tom) }
}