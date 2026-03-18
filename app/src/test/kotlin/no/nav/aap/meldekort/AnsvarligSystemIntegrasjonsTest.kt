package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

class AnsvarligSystemIntegrasjonsTest {
    private val idag = LocalDate.now()
    private val app = AppInstance(idag)

    @Test
    fun `ansvarlig system, ingen sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(fnr)
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i arena`() {
        val fnr = fødselsnummerGenerator.next()
        app.arenaSak(fnr)

        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()

        app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))
        app.kelvinSak(fnr)

        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i arena`() {
        val fnr = fødselsnummerGenerator.next()

        app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(1), idag.plusMonths(1)))
        app.kelvinSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))

        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
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
            FakeServers.start()

            GatewayRegistry
                .register<AapGatewayImpl>()

            embeddedServer = run {
                startHttpServer(
                    port = 0,
                    prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    applikasjonsVersjon = "TestApp",
                    tokenxConfig = TokenxConfig(),
                    azureConfig = AzureConfig(),
                    dataSource = dataSource,
                    wait = false,
                    repositoryRegistry = postgresRepositoryRegistry,
                    clock = Clock.systemDefaultZone(),
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
        }
    }
}
