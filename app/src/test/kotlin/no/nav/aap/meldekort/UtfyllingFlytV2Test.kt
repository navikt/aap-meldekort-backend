package no.nav.aap.meldekort

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
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.FraværSvarDto.GJENNOMFØRT_AVTALT_AKTIVITET
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
import no.nav.aap.utfylling.UtfyllingFlytNavn
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class UtfyllingFlytV2Test {
    val steg = UtfyllingFlytNavn.AAP_FLYT_V2.steg.filter { !it.erTeknisk }
    // TODO kan fjernes når vi har skrudd på V2 flyt i prod
    @BeforeEach
    fun setUp() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }
    @Test
    fun `går gjennom alle steg når det finnes fravær fra avtalt aktivitet`() {
        val idag = LocalDate.of(2025, 3, 1)
        clockHolder.idag = idag
        val fnr = fødselsnummerGenerator.next()
        val meldeperiode = Periode(6 januar 2025, 19 januar 2025)
        val sakStart = meldeperiode.fom

        kelvinSak(
            fnr,
            rettighetsperiode = Periode(sakStart, sakStart.plusWeeks(52)),
            opplysningsbehov = listOf(Periode(sakStart, sakStart.plusWeeks(52)))
        )

        val startUtfylling = myPost<StartUtfyllingResponse, StartUtfyllingRequest>(
            fnr, "/api/start-innsending", StartUtfyllingRequest(
                fom = meldeperiode.fom,
                tom = meldeperiode.tom
            )
        )

        val referanse = startUtfylling!!.metadata!!.referanse

        val dagerMedTimer = meldeperiode.copy(fom = sakStart).mapIndexed { index, dato ->
            DagSvarDto(
                dato = dato,
                timerArbeidet = (Math.random() * 3.0).toInt().toDouble(),
                fravær = if (index == 4) FraværDto.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN else null
            )
        }

        val flytSteg = listOf(
            lagTilstand(
                aktivtSteg = StegDto.INTRODUKSJON,
                harDuJobbet = null,
                harDuGjennomførtAvtaltAktivitet = null,
            ),
            lagTilstand(
                aktivtSteg = StegDto.SPØRSMÅL,
                dager = dagerMedTimer,
                harDuGjennomførtAvtaltAktivitet = FraværSvarDto.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET,
            ),
            lagTilstand(
                aktivtSteg = StegDto.UTFYLLING,
                dager = dagerMedTimer,
                harDuGjennomførtAvtaltAktivitet = FraværSvarDto.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET,
            ),
            lagTilstand(
                aktivtSteg = StegDto.FRAVÆR_UTFYLLING,
                dager = dagerMedTimer,
                harDuGjennomførtAvtaltAktivitet = FraværSvarDto.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET,
            ),
            lagTilstand(
                aktivtSteg = StegDto.BEKREFT,
                dager = dagerMedTimer,
                harDuGjennomførtAvtaltAktivitet = FraværSvarDto.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET,
            )
        )
        flytSteg.forEach { tilstand ->
            val response = myPost<UtfyllingResponseDto, EndreUtfyllingRequest>(
                fnr, "/api/utfylling/$referanse/lagre-neste", EndreUtfyllingRequest(tilstand)
            )
            assertNotEquals(tilstand.aktivtSteg, response?.tilstand?.aktivtSteg)
        }

    }

    @Test
    fun `formkrav brudd returner til feilende steg`() {
        val fnr = fødselsnummerGenerator.next()
        val meldeperiode = Periode(6 januar 2025, 19 januar 2025)
        val sakStart = meldeperiode.fom

        kelvinSak(
            fnr,
            rettighetsperiode = Periode(sakStart, sakStart.plusWeeks(52)),
            opplysningsbehov = listOf(Periode(sakStart, sakStart.plusWeeks(52)))
        )

        val startUtfylling = myPost<StartUtfyllingResponse, StartUtfyllingRequest>(
            fnr, "/api/start-innsending", StartUtfyllingRequest(
                fom = meldeperiode.fom,
                tom = meldeperiode.tom
            )
        )

        val referanse = startUtfylling!!.metadata!!.referanse

        val introduksjonMedFeil = lagTilstand(
            aktivtSteg = StegDto.INTRODUKSJON,
            vilSvareRiktig = false,
            harDuJobbet = null,
            harDuGjennomførtAvtaltAktivitet = null,
        )

        val response = myPost<UtfyllingResponseDto, EndreUtfyllingRequest>(
            fnr, "/api/utfylling/$referanse/lagre-neste", EndreUtfyllingRequest(introduksjonMedFeil)
        )

        assertEquals(StegDto.INTRODUKSJON, response?.tilstand?.aktivtSteg)
    }

    @Test
    fun `hopper over FRAVÆR_UTFYLLING når avtalt aktivitet er gjennomført`() {
        val fnr = fødselsnummerGenerator.next()
        val meldeperiode = Periode(6 januar 2025, 19 januar 2025)
        val sakStart = meldeperiode.fom
        val helePerioden = Periode(sakStart, sakStart.plusWeeks(52))

        kelvinSak(
            fnr,
            helePerioden,
            listOf(helePerioden)
        )

        val startUtfylling = myPost<StartUtfyllingResponse, StartUtfyllingRequest>(
            fnr, "/api/start-innsending", StartUtfyllingRequest(
                fom = meldeperiode.fom,
                tom = meldeperiode.tom
            )
        )

        val referanse = startUtfylling!!.metadata!!.referanse

        val dagerMedTimer = meldeperiode.copy(fom = sakStart).map {
            DagSvarDto(dato = it, timerArbeidet = 2.0, fravær = null)
        }

       val tilstand = lagTilstand(
            aktivtSteg = StegDto.UTFYLLING,
            dager = dagerMedTimer,
            harDuGjennomførtAvtaltAktivitet = GJENNOMFØRT_AVTALT_AKTIVITET
        )

        val response = myPost<UtfyllingResponseDto, EndreUtfyllingRequest>(
            fnr, "/api/utfylling/$referanse/lagre-neste", EndreUtfyllingRequest(tilstand)
        )

        assertEquals(StegDto.BEKREFT, response?.tilstand?.aktivtSteg)
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

        val standardSvar = SvarDto(
            vilSvareRiktig = true,
            harDuJobbet = true,
            dager = emptyList(),
            stemmerOpplysningene = true,
            harDuGjennomførtAvtaltAktivitet = GJENNOMFØRT_AVTALT_AKTIVITET
        )

        val standardTilstand = UtfyllingTilstandDto(
            aktivtSteg = StegDto.SPØRSMÅL,
            svar = standardSvar,
        )

        fun lagTilstand(
            aktivtSteg: StegDto = standardTilstand.aktivtSteg,
            svar: SvarDto? = null,
            vilSvareRiktig: Boolean? = standardSvar.vilSvareRiktig,
            harDuJobbet: Boolean? = standardSvar.harDuJobbet,
            dager: List<DagSvarDto> = standardSvar.dager,
            stemmerOpplysningene: Boolean? = standardSvar.stemmerOpplysningene,
            harDuGjennomførtAvtaltAktivitet: FraværSvarDto? = standardSvar.harDuGjennomførtAvtaltAktivitet,
        ) = UtfyllingTilstandDto(
            aktivtSteg = aktivtSteg,
            svar = svar ?: standardSvar.copy(
                vilSvareRiktig = vilSvareRiktig,
                harDuJobbet = harDuJobbet,
                dager = dager,
                stemmerOpplysningene = stemmerOpplysningene,
                harDuGjennomførtAvtaltAktivitet = harDuGjennomførtAvtaltAktivitet,
            ),
        )

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











