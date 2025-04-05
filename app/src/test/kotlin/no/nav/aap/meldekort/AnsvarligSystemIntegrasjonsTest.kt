package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.engine.EmbeddedServer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import kotlin.test.assertEquals
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Funker ikke i github action :(")
class AnsvarligSystemIntegrasjonsTest {
    private val idag = LocalDate.now()

    @Test
    fun `ansvarlig system, ingen sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        assertEquals("AAP", get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        kelvinSak(fnr)
        assertEquals("AAP", get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i arena`() {
        val fnr = fødselsnummerGenerator.next()
        arenaSak(fnr)

        assertEquals("FELLES", get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()

        arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))
        kelvinSak(fnr)

        assertEquals("AAP", get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i arena`() {
        val fnr = fødselsnummerGenerator.next()

        arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(1), idag.plusMonths(1)))
        kelvinSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))

        assertEquals("FELLES", get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    private fun kelvinSak(
        fnr: Ident,
        rettighetsperiode: Periode? = null,
    ) {
        val saksnummer = saksnummerGenerator.next()
        val sakenGjelderFor =
            rettighetsperiode ?: Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        dataSource.transaction { conn ->
            val sakRepo = KelvinSakRepositoryPostgres(conn)
            sakRepo.upsertSak(
                saksnummer = saksnummer,
                sakenGjelderFor = sakenGjelderFor,
                identer = listOf(fnr),
                meldeperioder = listOf(),
                meldeplikt = listOf(),
                opplysningsbehov = listOf(),
                status = null,
            )
        }
        FakeAapApi.upsert(
            fnr,
            FakeAapApi.FakeSak(
                referanse = FagsakReferanse(
                    FagsystemNavn.KELVIN,
                    saksnummer,
                ),
                rettighetsperiode = sakenGjelderFor,
            )
        )
    }

    private fun arenaSak(
        fnr: Ident,
        sak: Fagsaknummer? = null,
        rettighetsperiode: Periode? = null,
    ) {
        FakeAapApi.upsert(
            fnr,
            FakeAapApi.FakeSak(
                referanse = FagsakReferanse(
                    FagsystemNavn.ARENA,
                    (sak ?: saksnummerGenerator.next()),
                ),
                rettighetsperiode = rettighetsperiode ?: Periode(idag.minusMonths(1), idag.plusMonths(1)),
            )
        )
    }

    companion object {
        private lateinit var embeddedServer: EmbeddedServer<*, *>
        lateinit var client: RestClient<InputStream>

        val dataSource = createTestcontainerPostgresDataSource(prometheus)

        val baseUrl: String
            get() = "http://localhost:${embeddedServer.port()}"

        inline fun <reified T> get(fnr: Ident, path: String): T? {
            return client.get<T>(
                URI("$baseUrl$path"), GetRequest(
                    additionalHeaders = listOf(
                        Header("Authorization", "Bearer ${FakeTokenX.issueToken(fnr.asString)}")
                    )
                )
            )
        }


        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            FakeTokenX.port = 0
            FakeServers.start()

            setupRegistries()

            embeddedServer = run {
                setupRegistries()
                startHttpServer(
                    port = 0,
                    prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    applikasjonsVersjon = "TestApp",
                    tokenxConfig = TokenxConfig(),
                    azureConfig = AzureConfig(),
                    dataSource = dataSource,
                    wait = false,
                    repositoryRegistry = postgresRepositoryRegistry,
                )
            }

            client = RestClient.withDefaultResponseHandler(
                config = ClientConfig(),
                tokenProvider = object : TokenProvider {},
            )
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            embeddedServer.stop(0L, 0L)
            FakeServers.close()
        }
    }
}
