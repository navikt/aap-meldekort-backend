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
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * 2024
 *         July                     August                  September
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *   1  2  3  4  5  6  7                1  2  3  4                         1
 *   8  9 10 11 12 13 14       5  6  7  8  9 10 11       2  3  4  5  6  7  8
 *  15 16 17 18 19 20 21      12 13 14 15 16 17 18       9 10 11 12 13 14 15
 *  22 23 24 25 26 27 28      19 20 21 22 23 24 25      16 17 18 19 20 21 22
 *  29 30 31                  26 27 28 29 30 31         23 24 25 26 27 28 29
 *                                                      30
 *
 *
 *       October                   November                  December
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *      1  2  3  4  5  6                   1  2  3                         1
 *   7  8  9 10 11 12 13       4  5  6  7  8  9 10       2  3  4  5  6  7  8
 *  14 15 16 17 18 19 20      11 12 13 14 15 16 17       9 10 11 12 13 14 15
 *  21 22 23 24 25 26 27      18 19 20 21 22 23 24      16 17 18 19 20 21 22
 *  28 29 30 31               25 26 27 28 29 30         23 24 25 26 27 28 29
 *                                                      30 31
 *
 *  2025
 *
 *       January                   February                   March
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *         1  2  3  4  5                      1  2                      1  2
 *  [6] 7  8  9 10 11 12       3  4  5  6  7  8  9       3  4  5  6  7  8  9
 *  13 14 15 16 17 18 19      10 11 12 13 14 15 16      10 11 12 13 14 15 16
 *  20 21 22 23 24 25 26      17 18 19 20 21 22 23      17 18 19 20 21 22 23
 *  27 28 29 30 31            24 25 26 27 28            24 25 26 27 28 29 30
 *                                                      31
 *
 *
 *        April                      May                       June
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *      1  2  3  4  5  6                1  2  3  4                         1
 *   7  8  9 10 11 12 13       5  6  7  8  9 10 11       2  3  4  5  6  7  8
 *  14 15 16 17 18 19 20      12 13 14 15 16 17 18       9 10 11 12 13 14 15
 *  21 22 23 24 25 26 27      19 20 21 22 23 24 25      16 17 18 19 20 21 22
 *  28 29 30                  26 27 28 29 30 31         23 24 25 26 27 28 29
 *                                                      30
 */

private val idag = 6 januar 2025

@Disabled("Funker ikke i github action :(")
class KelvinIntegrasjonsPåFastsattDagTest {
    @Test
    fun `ikke vedtak, på dagen`() {
        val fnr = fødselsnummerGenerator.next()
        val meldeperiode1 = Periode(6 januar 2025, 19 januar 2025)
        val meldevindu1 = Periode(20 januar 2025, 27 januar 2025)
        kelvinSak(fnr, rettighetsperiode = Periode(idag, idag.plusWeeks(52)))
        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `ikke vedtak, en uke etter søknad`() {
        val fnr = fødselsnummerGenerator.next()
        kelvinSak(fnr, rettighetsperiode = Periode(idag.minusWeeks(1), idag.plusWeeks(51)))
        val meldeperiode1 = Periode(30 desember 2024, 12 januar 2025)
        val meldevindu1 = Periode(13 januar 2025, 20 januar 2025)
        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `ikke vedtak, tre uker etter søknad`() {
        val fnr = fødselsnummerGenerator.next()
        kelvinSak(fnr, rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(51)))
        val meldeperiode1 = Periode(16 desember 2024, 29 desember 2024)
        val meldevindu1 = Periode(30 desember 2024, 6 januar 2025)
        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(meldeperiode1, it, "/manglerOpplysninger")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `med vedtak, tre uker etter søknad`() {
        val fnr = fødselsnummerGenerator.next()
        kelvinSak(fnr,
            rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(49)),
            opplysningsbehov = listOf(Periode(16 desember 2024, idag.plusWeeks(49)))
        )
        val meldeperiode1 = Periode(16 desember 2024, 29 desember 2024)
        val meldevindu1 = Periode(30 desember 2024, 6 januar 2025)
        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(meldeperiode1, it, "/manglerOpplysninger")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `med vedtak, en uke etter søknad med opplysningsbehov i dag`() {
        val fnr = fødselsnummerGenerator.next()
        kelvinSak(fnr,
            rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(51)),
            opplysningsbehov = listOf(Periode(16 desember 2024, idag.plusWeeks(51)))
        )
        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt("2024-12-16", it, "/manglerOpplysninger/fom")
            assertEqualsAt("2024-12-29", it, "/manglerOpplysninger/tom")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2024-12-16", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2024-12-29", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2024-12-30", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-01-06", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
    }

    private fun kelvinSak(
        fnr: Ident,
        rettighetsperiode: Periode? = null,
        opplysningsbehov: List<Periode>? = null,
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
                meldeperioder = sequence {
                    var fom = sakenGjelderFor.fom.minusDays(sakenGjelderFor.fom.dayOfWeek.value - 1L)
                    while (fom <= sakenGjelderFor.tom) {
                        yield(Periode(fom, fom.plusDays(13)))
                        fom = fom.plusDays(14)
                    }
                }.toList().also {println(it)},
                meldeplikt = listOf(),
                opplysningsbehov = opplysningsbehov ?: listOf(sakenGjelderFor),
            )
        }
        FakeAapApi.upsert(
            fnr,
            Sak(
                referanse = FagsakReferanse(
                    FagsystemNavn.KELVIN,
                    saksnummer,
                ),
                rettighetsperiode = sakenGjelderFor,
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
                    dagensDato = idag,
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

fun assertEqualsAt(expected: Any?, json: JsonNode, path: String) {
    val expectedNormalized = when {
        expected is LocalDate -> expected.toString()
        else -> expected
    }
    val obj = json.at(path)
    val result = when {
        obj.isNull -> null
        obj.isTextual -> obj.asText()
        obj.isInt -> obj.asInt()
        expectedNormalized is Periode ->
            Periode(LocalDate.parse(obj["fom"].asText()), LocalDate.parse(obj["tom"].asText()))
        else -> error("uforventet type ${obj.javaClass.name}")
    }

    assertEquals(expectedNormalized, result)
}
