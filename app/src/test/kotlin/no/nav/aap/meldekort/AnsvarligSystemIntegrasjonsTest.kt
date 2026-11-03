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
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTexas
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AnsvarligSystemIntegrasjonsTest {
    private val idag = LocalDate.now()
    private val app = AppInstance(idag)

    @Nested
    inner class AnsvarligSystemV2 {
        @Test
        fun `ingen saker gir AAP uten saksnummer og tom rettighetsperiode`() {
            val fnr = fødselsnummerGenerator.next()

            val response = assertNotNull(app.get<JsonNode>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals("""{"løsninger":[{"system":"AAP","saksnummer":null,"rettighetsperiode":null}]}""", response.toString())
        }

        @Test
        fun `arena alene gir FELLES med saksnummer og rettighetsperiode`() {
            val fnr = fødselsnummerGenerator.next()
            val saksnummer = saksnummerGenerator.next()
            val periode = Periode(idag, idag)
            app.arenaSak(fnr, sak = saksnummer, rettighetsperiode = periode)

            val response = assertNotNull(app.get<JsonNode>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals(
                """{"løsninger":[{"system":"FELLES","saksnummer":"${saksnummer.asString}","rettighetsperiode":{"fom":"${periode.fom}","tom":"${periode.tom}"}}]}""",
                response.toString(),
            )
        }

        @Test
        fun `kelvin gir bare AAP når arena ikke dekker dagen før kelvin`() {
            val fnr = fødselsnummerGenerator.next()
            val periode = Periode(idag.minusDays(1), idag.plusDays(1))
            val saksnummer = app.kelvinSak(fnr, rettighetsperiode = periode)
            app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(4), idag.minusDays(3)))

            val response = assertNotNull(app.get<JsonNode>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals(
                """{"løsninger":[{"system":"AAP","saksnummer":"${saksnummer.asString}","rettighetsperiode":{"fom":"${periode.fom}","tom":"${periode.tom}"}}]}""",
                response.toString(),
            )
        }

        @Test
        fun `kelvin og forutgående arena gir både AAP og FELLES`() {
            val fnr = fødselsnummerGenerator.next()
            val kelvinPeriode = Periode(idag.minusDays(10), idag.plusDays(10))
            val kelvinSaksnummer = app.kelvinSak(fnr, rettighetsperiode = kelvinPeriode)
            val arenaPeriode = Periode(idag.minusMonths(4), kelvinPeriode.fom.minusDays(1))
            val arenaSaksnummer = saksnummerGenerator.next()
            app.arenaSak(fnr, sak = arenaSaksnummer, rettighetsperiode = arenaPeriode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals(2, response.løsninger.size)
            val aap = response.løsninger[0]
            assertEquals(AnsvarligMeldekortløsningDto.AAP, aap.system)
            assertEquals(kelvinSaksnummer.asString, aap.saksnummer)
            val aapPeriode = assertNotNull(aap.rettighetsperiode)
            assertEquals(kelvinPeriode.fom, aapPeriode.fom)
            assertEquals(kelvinPeriode.tom, aapPeriode.tom)
            val felles = response.løsninger[1]
            assertEquals(AnsvarligMeldekortløsningDto.FELLES, felles.system)
            assertEquals(arenaSaksnummer.asString, felles.saksnummer)
            val fellesPeriode = assertNotNull(felles.rettighetsperiode)
            assertEquals(arenaPeriode.fom, fellesPeriode.fom)
            assertEquals(arenaPeriode.tom, fellesPeriode.tom)
        }

        @Test
        fun `historisk arena uten aktiv kelvin gir AAP som standard`() {
            val fnr = fødselsnummerGenerator.next()
            app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(4), idag.minusDays(1)))

            val response = assertNotNull(app.get<JsonNode>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals("""{"løsninger":[{"system":"AAP","saksnummer":null,"rettighetsperiode":null}]}""", response.toString())
        }

        @Test
        fun `fremtidig kelvin endrer ikke arena-oppslag for dagens dato`() { // TODO jeg er usikker på om denne er korrekt, hva er rettighetsperioden for en sak som ikke er innvilget enda?
            val fnr = fødselsnummerGenerator.next()
            app.kelvinSak(fnr, rettighetsperiode = Periode(idag.plusWeeks(2), idag.plusWeeks(6)))
            val arenaSaksnummer = saksnummerGenerator.next()
            app.arenaSak(fnr, sak = arenaSaksnummer, rettighetsperiode = Periode(idag, idag))

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            val løsning = response.løsninger.single()
            assertEquals(AnsvarligMeldekortløsningDto.FELLES, løsning.system)
            assertEquals(arenaSaksnummer.asString, løsning.saksnummer)
        }

        @Test
        fun `Sak i Kelvin med første dag i fremtiden AAP uten saksnummer`() {
            val periode = Periode(idag.plusWeeks(2), idag.plusWeeks(6))
            val fnr = fødselsnummerGenerator.next()
            app.kelvinSak(fnr, rettighetsperiode = periode)

            val response = assertNotNull(app.get<JsonNode>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals("""{"løsninger":[{"system":"AAP","saksnummer":null,"rettighetsperiode":null}]}""", response.toString())
        }
    }

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
                        Header("Authorization", "Bearer ${FakeTexas.issueToken(fnr.asString)}")
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
