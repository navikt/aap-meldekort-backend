package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.saker.ArenaoppslagGatewayImpl
import no.nav.aap.meldekort.test.FakeArenaoppslag
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTexas
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AnsvarligSystemIntegrasjonsTest {
    private val idag = LocalDate.now()
    private val app = AppInstance(idag)

    @Nested
    inner class AnsvarligSystemV2 {
        @Test
        fun `v2 bruker arenaoppslag mens eldre endepunkt fortsatt bruker api-intern`() {
            val fnr = fødselsnummerGenerator.next()
            FakeArenaoppslag.settVedtak(
                fnr,
                listOf(
                    ArenaVedtak(
                        sakId = 1234,
                        statusKode = "IVERK",
                        vedtaktypeKode = "O",
                        fraOgMed = idag,
                        tilDato = idag,
                        rettighetkode = "AAP",
                        utfallkode = "JA",
                        vedtakId = 1,
                        aktivitetsfaseKode = "AU",
                    ),
                ),
            )

            assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
            assertNull(FakeArenaoppslag.forespørsler[fnr.asString])

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.aap)
            val løsning = assertNotNull(response.felles)
            assertEquals("1234", løsning.saksnummer)
            val periode = assertNotNull(løsning.rettighetsperiode)
            assertEquals(idag, periode.fom)
            assertEquals(idag, periode.tom)
            assertEquals(VedtakForPersonRequest(fnr.asString), FakeArenaoppslag.forespørsler[fnr.asString])
        }

        @Test
        fun `overlappende arena-vedtak velger perioden med senest sluttdato`() {
            val fnr = fødselsnummerGenerator.next()
            app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusDays(1), idag))
            val saksnummer = saksnummerGenerator.next()
            val periode = Periode(idag, idag.plusDays(1))
            app.arenaSak(fnr, sak = saksnummer, rettighetsperiode = periode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.aap)
            val løsning = assertNotNull(response.felles)
            assertEquals(saksnummer.asString, løsning.saksnummer)
            val responsPeriode = assertNotNull(løsning.rettighetsperiode)
            assertEquals(periode.fom, responsPeriode.fom)
            assertEquals(periode.tom, responsPeriode.tom)
        }

        @Test
        fun `feil i arenaoppslag propagerer til api-et`() {
            val fnr = fødselsnummerGenerator.next()
            app.kelvinSak(fnr)
            FakeArenaoppslag.settRespons(fnr, HttpStatusCode.InternalServerError, "Upstream failure")

            assertFailsWith<InternalServerErrorHttpResponsException> {
                app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system")
            }
        }

        @Test
         fun `når det ikke finnes sak i hverken Arena eller Kelvin er begge løsningene satt til null`() {
            val fnr = fødselsnummerGenerator.next()

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.aap)
            assertNull(response.felles)
        }

        @Test
        fun `når bruker kun har et aktivt vedtak i Arena returneres felles med saksnummer og rettighetsperiode fra Arena`() {
            val fnr = fødselsnummerGenerator.next()
            val saksnummer = saksnummerGenerator.next()
            val periode = Periode(idag, idag)
            app.arenaSak(fnr, sak = saksnummer, rettighetsperiode = periode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))
            assertNull(response.aap)
            assertEquals(saksnummer.asString, response.felles?.saksnummer)
            assertEquals( periode.fom,response.felles?.rettighetsperiode?.fom)
            assertEquals( periode.tom,response.felles?.rettighetsperiode?.tom)
        }

        @Test
        fun `gir bare AAP når vedtak i arena slutter 8 dager eller mer før start i Kelvin`() {
            val fnr = fødselsnummerGenerator.next()
            val periode = Periode(idag.minusDays(1), idag.plusDays(1))
            val saksnummer = app.kelvinSak(fnr, rettighetsperiode = periode)
            app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(4), periode.fom.minusDays(8)))

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertEquals(saksnummer.asString, response.aap?.saksnummer)
            assertEquals(periode.fom, response.aap?.rettighetsperiode?.fom)
            assertEquals(periode.tom, response.aap?.rettighetsperiode?.tom)

            assertNull(response.felles)
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 7])
        fun `arena-vedtak som har siste dato maks 7 dager før start i kelvin gir både AAP og FELLES`(dagerFørKelvin: Int) {
            val fnr = fødselsnummerGenerator.next()
            val kelvinPeriode = Periode(idag.minusDays(10), idag.plusDays(10))
            val kelvinSaksnummer = app.kelvinSak(fnr, rettighetsperiode = kelvinPeriode)
            val arenaDato = kelvinPeriode.fom.minusDays(dagerFørKelvin.toLong())
            val arenaPeriode = Periode(kelvinPeriode.fom.minusMonths(1), arenaDato)
            val arenaSaksnummer = saksnummerGenerator.next()
            app.arenaSak(fnr, sak = arenaSaksnummer, rettighetsperiode = arenaPeriode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            val aap = assertNotNull(response.aap)
            assertEquals(kelvinSaksnummer.asString, aap.saksnummer)
            val felles = assertNotNull(response.felles)
            assertEquals(arenaSaksnummer.asString, felles.saksnummer)
            val periode = assertNotNull(felles.rettighetsperiode)
            assertEquals(arenaPeriode.fom, periode.fom)
            assertEquals(arenaDato, periode.tom)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 10])
         fun `dersom en sak i Arena slutter på eller etter start-dato i Kelvin viser vi kun info om sak i Kelvin`(dagerEtterKelvinStart: Int) {
            // dette er vel et ikke-tilfelle, men hvis situasjonen oppstår, hvordan vil vi håndtere den? Jeg tror vi må vise begge, men lar denne testen være inntil videre
            val fnr = fødselsnummerGenerator.next()
            val kelvinPeriode = Periode(idag.minusDays(10), idag.plusDays(10))
            val saksnummer = app.kelvinSak(fnr, rettighetsperiode = kelvinPeriode)
            app.arenaSak(
                fnr,
                rettighetsperiode = Periode(
                    kelvinPeriode.fom.minusMonths(1),
                    kelvinPeriode.fom.plusDays(dagerEtterKelvinStart.toLong()),
                ),
            )

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.felles)
            val løsning = assertNotNull(response.aap)
            assertEquals(saksnummer.asString, løsning.saksnummer)
        }

        @Test
        fun `arena uten sluttdato gir bare AAP med aktiv kelvin-sak`() {
            val fnr = fødselsnummerGenerator.next()
            val kelvinPeriode = Periode(idag.minusDays(10), idag.plusDays(10))
            val saksnummer = app.kelvinSak(fnr, rettighetsperiode = kelvinPeriode)
            FakeArenaoppslag.settVedtak(
                fnr,
                listOf(
                    ArenaVedtak(
                        sakId = 1234,
                        statusKode = "IVERK",
                        vedtaktypeKode = "O",
                        fraOgMed = kelvinPeriode.fom.minusMonths(1),
                        tilDato = null,
                        rettighetkode = "AAP",
                        utfallkode = "JA",
                        vedtakId = 1,
                        aktivitetsfaseKode = "AU",
                    ),
                ),
            )

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.felles)
            val løsning = assertNotNull(response.aap)
            assertEquals(saksnummer.asString, løsning.saksnummer)
        }

        @Test
        fun `når det finnes flere perioden i Arena i uken før vedtak starter i Kelvin velger vi den seneste sluttdatoen`() {
            val fnr = fødselsnummerGenerator.next()
            val kelvinPeriode = Periode(idag.minusDays(10), idag.plusDays(10))
            app.kelvinSak(fnr, rettighetsperiode = kelvinPeriode)
            app.arenaSak(
                fnr,
                rettighetsperiode = Periode(kelvinPeriode.fom.minusDays(7), kelvinPeriode.fom.minusDays(5)),
            )
            val saksnummer = saksnummerGenerator.next()
            val arenaPeriode = Periode(kelvinPeriode.fom.minusDays(3), kelvinPeriode.fom.minusDays(2))
            app.arenaSak(fnr, sak = saksnummer, rettighetsperiode = arenaPeriode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNotNull(response.aap)
            val felles = assertNotNull(response.felles)
            assertEquals(saksnummer.asString, felles.saksnummer)
            val periode = assertNotNull(felles.rettighetsperiode)
            assertEquals(arenaPeriode.fom, periode.fom)
            assertEquals(arenaPeriode.tom, periode.tom)
        }

        @Test
        fun `historisk sak i Arena, men ingen sak i Kelvin gir tomt svar for begge løsninger`() {
            val fnr = fødselsnummerGenerator.next()
            app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(4), idag.minusDays(1)))

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))
            assertNull(response.aap)
            assertNull(response.felles)
        }

        @Test
        fun `fremtidig kelvin endrer ikke arena-oppslag for dagens dato`() {
            val fnr = fødselsnummerGenerator.next()
            app.kelvinSak(fnr, rettighetsperiode = Periode(idag.plusWeeks(2), idag.plusWeeks(6)))
            val arenaSaksnummer = saksnummerGenerator.next()
            app.arenaSak(fnr, sak = arenaSaksnummer, rettighetsperiode = Periode(idag, idag))

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))

            assertNull(response.aap)
            val løsning = assertNotNull(response.felles)
            assertEquals(arenaSaksnummer.asString, løsning.saksnummer)
        }

        @Test
        fun `fremtidig sak i kelvin og ingen sak arena gir null for begge løsninger`() {
            val periode = Periode(idag.plusWeeks(2), idag.plusWeeks(6))
            val fnr = fødselsnummerGenerator.next()
            app.kelvinSak(fnr, rettighetsperiode = periode)

            val response = assertNotNull(app.get<AnsvarligeMeldekortløsningerDto>(fnr, "/api/v2/ansvarlig-system"))
            assertNull(response.aap)
            assertNull(response.felles)
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
                .register<ArenaoppslagGatewayImpl>()

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
