package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.journalføring.PdfgenGatewayImpl
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.time.LocalDate

/**
 * ```
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
 * ```
 */


class KelvinIntegrasjonManuellInnsendingTest {
    @Test
    fun `Kunne sende inn papirmeldekort fra behandlingsflyt imellom elektroniske innsendinger`() {
        val idag = LocalDate.of(2025, 12, 1)
        clockHolder.idag = idag

        val fnr = fødselsnummerGenerator.next()
        val rettighetsperiode = Periode(17 november 2025, idag.plusWeeks(51))
        val fagsaknummer = kelvinSak(
            fnr,
            rettighetsperiode = rettighetsperiode,
            opplysningsbehov = listOf(Periode(17 november 2025, idag.plusWeeks(51)))
        )

        fyllInnTimer(
            fnr,
            opplysningerOm = Periode(17 november 2025, 30 november 2025),
            sakStart = rettighetsperiode.fom
        )

        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-01", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-14", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-12-22", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        clockHolder.idag = LocalDate.of(2025, 12, 15)
        fyllInnTimer(
            fnr,
            opplysningerOm = Periode(1 desember 2025, 14 desember 2025),
            sakStart = rettighetsperiode.fom
        )

        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-28", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-05", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        clockHolder.idag = LocalDate.of(2025, 12, 29)

        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-15", it, "/manglerOpplysninger/fom")
            assertEqualsAt("2025-12-28", it, "/manglerOpplysninger/tom")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-28", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-05", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        fyllInnTimerFraBehandlingsflyt(
            fnr, fagsaknummer.asString, Periode(15 desember 2025, 28 desember 2025)
        )

        get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2026-01-11", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2026-01-12", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-19", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
    }

    private fun fyllInnTimerFraBehandlingsflyt(
        fnr: Ident,
        saksnummer: String,
        periode: Periode,
    ) {
        val dagerJobbet = periode.map {
            DagSvarDto(
                dato = it,
                timerArbeidet = (Math.random() * 3.0).toInt().toDouble()
            )
        }

        val request = BehandslingsflytUtfyllingRequest(
            saksnummer = saksnummer,
            ident = fnr.asString,
            periode = periode,
            harDuJobbet = true,
            dager = dagerJobbet
        )

        myPost<Unit, BehandslingsflytUtfyllingRequest>(
            fnr = fnr,
            path = "/api/behandlingsflyt/sak/timer",
            body = request
        )
    }

    private fun fyllInnTimer(
        fnr: Ident,
        opplysningerOm: Periode,
        sakStart: LocalDate = opplysningerOm.fom
    ) {
        val startUtfylling = myPost<StartUtfyllingResponse, StartUtfyllingRequest>(
            fnr, "/api/start-innsending", StartUtfyllingRequest(
                fom = sakStart,
                tom = opplysningerOm.tom
            )
        )

        val referanse = startUtfylling!!.metadata!!.referanse

        val dagerJobbet = opplysningerOm.copy(fom = sakStart).map {
            DagSvarDto(
                dato = it,
                timerArbeidet = (Math.random() * 3.0).toInt().toDouble()
            )
        }
        val utfyllinger = listOf(
            UtfyllingTilstandDto(
                aktivtSteg = StegDto.INTRODUKSJON,
                svar = SvarDto(
                    vilSvareRiktig = true,
                    harDuJobbet = null,
                    dager = emptyList(),
                    stemmerOpplysningene = true
                )
            ), UtfyllingTilstandDto(
                aktivtSteg = StegDto.SPØRSMÅL,
                svar = SvarDto(
                    vilSvareRiktig = true,
                    harDuJobbet = true,
                    dager = dagerJobbet,
                    stemmerOpplysningene = true
                )
            ),
            UtfyllingTilstandDto(
                aktivtSteg = StegDto.UTFYLLING,
                svar = SvarDto(
                    vilSvareRiktig = true,
                    harDuJobbet = true,
                    dager = dagerJobbet,
                    stemmerOpplysningene = true
                )
            ), UtfyllingTilstandDto(
                aktivtSteg = StegDto.BEKREFT,
                svar = SvarDto(
                    vilSvareRiktig = true,
                    harDuJobbet = true,
                    dager = dagerJobbet,
                    stemmerOpplysningene = true
                )
            )
        )

        utfyllinger.forEach {
            myPost<UtfyllingResponseDto, EndreUtfyllingRequest>(
                fnr, "/api/utfylling/$referanse/lagre-neste", EndreUtfyllingRequest(it)
            )
        }
    }

    private fun kelvinSak(
        fnr: Ident,
        rettighetsperiode: Periode? = null,
        opplysningsbehov: List<Periode>? = null,
    ): Fagsaknummer {
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
                }.toList().also { println(it) },
                meldeplikt = listOf(),
                opplysningsbehov = opplysningsbehov ?: listOf(sakenGjelderFor),
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
        return saksnummer
    }

    @BeforeEach
    fun beforeEach() {
        clockHolder.idag = idag
    }

    companion object {
        private val idag = 6 januar 2025

        val clockHolder = ClockHolder(idag)

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

        inline fun <reified T, B : Any> myPost(fnr: Ident, path: String, body: B): T? {
            return client.post<B, T>(
                URI("$baseUrl$path"),
                PostRequest(
                    additionalHeaders = listOf(
                        Header("Authorization", "Bearer ${FakeTokenX.issueToken(fnr.asString)}")
                    ),
                    body = body
                )
            )
        }


        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            FakeTokenX.port = 0
            FakeServers.start()

            GatewayRegistry
                .register<AapGatewayImpl>()
                .register<DokarkivGatewayImpl>()
                .register<PdfgenGatewayImpl>()
                .register<FakeVarselGateway>()

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
                    clock = clockHolder,
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