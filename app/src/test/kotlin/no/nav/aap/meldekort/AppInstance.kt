package no.nav.aap.meldekort

import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinMottakService
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.kelvin.KelvinSakService
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.journalføring.PdfgenGatewayImpl
import no.nav.aap.meldekort.meldekort.DefaultMeldekortServiceGateway
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.meldekort.test.port
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepositoryPostgres
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.prometheus
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.AktivitetsInformasjon
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import no.nav.aap.varsel.VarselRepositoryPostgres
import no.nav.aap.varsel.VarselService
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate

class AppInstance(initIdag: LocalDate = 6 januar 2025) : AutoCloseable {

    private val clockHolder = ClockHolder(initIdag)

    var idag: LocalDate
        get() = clockHolder.idag
        set(idag) { clockHolder.idag = idag }

    val dataSource = createTestcontainerPostgresDataSource(prometheus)

    init {
        FakeTokenX.port = 0
        FakeServers.start()
        System.setProperty("aap.meldekort.lenke", "https://aap-meldekort.ansatt.dev.nav.no/aap/meldekort")

        GatewayRegistry
            .register<AapGatewayImpl>()
            .register<DokarkivGatewayImpl>()
            .register<PdfgenGatewayImpl>()
            .register<DefaultMeldekortServiceGateway>()
            .register<FakeVarselGateway>()
    }

    private val embeddedServer: EmbeddedServer<*, *> = startHttpServer(
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

    val client: RestClient<InputStream> = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = "meldekort-backend"),
        tokenProvider = ClientCredentialsTokenProvider,
    )

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

    inline fun <reified R, B : Any> post(fnr: Ident, path: String, body: B): R? {
        return client.post<B, R>(
            URI("$baseUrl$path"),
            PostRequest(
                additionalHeaders = listOf(
                    Header("Authorization", "Bearer ${FakeTokenX.issueToken(fnr.asString)}")
                ),
                body = body,
                currentToken = getToken()
            )
        )
    }

    inline fun <reified T, B : Any> behandlingsflytPost(body: B): T? {
             return client.post<_, T>(
                 URI("${baseUrl}/api/behandlingsflyt/sak/meldeperioder"), PostRequest(
                     body = body,
                     currentToken = getToken()
                 )
             )
    }

    fun getToken(): OidcToken {
        val client = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = NoTokenTokenProvider(),
            responseHandler = DefaultResponseHandler()
        )
        return OidcToken(
            client.post<String, FakeServers.TestToken>(
                URI(requiredConfigForKey("azure.openid.config.token.endpoint")),
                PostRequest("grant_type=client_credentials"),
            )!!.access_token
        )
    }

    fun kelvinSak(
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

    fun arenaSak(
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

    fun arenaMeldekort(fnr: Ident, meldekortListe: List<ArenaMeldekort>) {
        no.nav.aap.meldekort.test.FakeArena.upsertMeldekort(fnr.asString, meldekortListe)
    }

    fun fyllInnTimer(
        fnr: Ident,
        opplysningerOm: Periode,
        sakStart: LocalDate = opplysningerOm.fom
    ) {
        val startUtfylling = post<StartUtfyllingResponse, StartUtfyllingRequest>(
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
            post<UtfyllingResponseDto, EndreUtfyllingRequest>(
                fnr, "/api/utfylling/$referanse/lagre-neste", EndreUtfyllingRequest(it)
            )
        }
    }

    fun hentUtfyllinger(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling? {
        return dataSource.transaction { conn ->
            val utfyllingRepository = UtfyllingRepositoryPostgres(conn)
            utfyllingRepository.lastUtfylling(ident, utfyllingReferanse)
        }
    }

    fun fyllInnTimerFraBehandlingsflyt(
        fnr: Ident,
        sakenGjelderFor: Periode,
        periode: Periode
    ): UtfyllingReferanse {
        return dataSource.transaction { connection ->
            val kelvinMottakService = kelvinMottakService(connection, clockHolder)

            val dagerJobbet = periode.map {
                DagSvarDto(
                    dato = it,
                    timerArbeidet = (Math.random() * 3.0).toInt().toDouble()
                )
            }

            kelvinMottakService.behandleMottatteAktivitetsInformasjon(
                ident = fnr,
                sakenGjelderFor = sakenGjelderFor,
                periode = periode,
                harDuJobbet = true,
                aktivitetsInformasjon = dagerJobbet.map { AktivitetsInformasjon(dato = it.dato, timer = it.timerArbeidet) }
            )
        }
    }


    fun kelvinMottakService(connection: DBConnection, clock: Clock): KelvinMottakService {
        return KelvinMottakService(
            varselService = varselService(connection, clock),
            kelvinSakRepository = KelvinSakRepositoryPostgres(connection),
            utfyllingRepository = UtfyllingRepositoryPostgres(connection),
            aktivitetsInformasjonRepository = AktivitetsInformasjonRepositoryPostgres(connection),
            clock = clock
        )
    }

    fun varselService(connection: DBConnection, clock: Clock): VarselService {
        val aktivitetsInformasjonRepository = AktivitetsInformasjonRepositoryPostgres(connection)
        val kelvinSakRepository = KelvinSakRepositoryPostgres(connection)
        return VarselService(
            kelvinSakService = KelvinSakService(
                kelvinSakRepository = kelvinSakRepository,
                aktivitetsInformasjonRepository = aktivitetsInformasjonRepository,
                clock = clock
            ),
            kelvinSakRepository = kelvinSakRepository,
            varselRepository = VarselRepositoryPostgres(connection),
            utfyllingRepository = UtfyllingRepositoryPostgres(connection),
            varselGateway = FakeVarselGateway,
            clock = clock
        )
    }


    override fun close() {
        embeddedServer.stop(0L, 0L)
    }
}