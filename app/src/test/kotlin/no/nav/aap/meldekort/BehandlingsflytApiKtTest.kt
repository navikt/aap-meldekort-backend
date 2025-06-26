package no.nav.aap.meldekort

import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.Fagsaknummer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.*

    class BehandlingsflytApiKtTest {

    @Test
    fun `meldeplikt settes korrekt i databasen ved melding fra behandlingsflyt`() {
        val rettighetsperiode = Periode(fom = LocalDate.of(2025, 3, 3), tom = LocalDate.of(2025, 4, 27))

        val meldeperioder = listOf(
            "2025-03-03" to "2025-03-16",
            "2025-03-17" to "2025-03-30",
            "2025-03-31" to "2025-04-13",
            "2025-04-14" to "2025-04-27"
        ).map{ (fom, tom) -> Periode(LocalDate.parse(fom), LocalDate.parse(tom))}

        val meldeplikt = listOf(
            "2025-03-31" to "2025-04-07",
            "2025-04-14" to "2025-04-21",
            "2025-04-28" to "2025-05-05"
        ).map { (fom, tom) -> Periode(LocalDate.parse(fom), LocalDate.parse(tom))}

        val fnr = fødselsnummerGenerator.next()
        val meldedata = MeldeperioderV0(
            sakenGjelderFor = rettighetsperiode,
            saksnummer = "SAKSNUMMER",
            identer = listOf(fnr.asString),
            meldeperioder = meldeperioder,
            meldeplikt = meldeplikt
        )

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

        post<Unit>(meldedata, getToken())
        dataSource.transaction { dbConnection ->
            val kelvinSakRepository = postgresRepositoryRegistry.provider(dbConnection).provide<KelvinSakRepository>()
            val meldepliktFraRepository = kelvinSakRepository.hentMeldeplikt(fnr, Fagsaknummer("SAKSNUMMER"))
            assertEquals(meldepliktFraRepository.map { Periode(it.fom, it.tom) }, meldeplikt)

            val meldeperioderFraRepository = kelvinSakRepository.hentMeldeperioder(fnr, Fagsaknummer("SAKSNUMMER"))
            assertEquals(meldeperioderFraRepository.map { Periode(it.fom, it.tom)}, meldeperioder)
        }
    }

     companion object {
         private lateinit var embeddedServer: EmbeddedServer<*, *>
         lateinit var client: RestClient<InputStream>

         val dataSource = createTestcontainerPostgresDataSource(prometheus)

         val baseUrl: String
             get() = "http://localhost:${embeddedServer.port()}"

//         val fakeToken:OidcToken? = null
//         fun getToken(): OidcToken {
//            val client = RestClient(
//                config = ClientConfig(scope = "behandlingsflyt"),
//                tokenProvider = NoTokenTokenProvider(),
//                responseHandler = DefaultResponseHandler()
//            )
//             return fakeToken ?: OidcToken(
//                 client.post<String, FakeServers.TestToken>(
//                     URI(requiredConfigForKey("azure.openid.config.token.endpoint")),
//                     PostRequest("grant_type=client_credentials"),
//                 )!!.access_token
//             )
//         }

         inline fun <reified T> post(meldedata: MeldeperioderV0, token: OidcToken): T? {
             return client.post<_, T>(
                 URI("${baseUrl}/api/behandlingsflyt/sak/meldeperioder"), PostRequest(
                     body = meldedata,
                     currentToken = token
                 )
             )
         }

         @JvmStatic
         @BeforeAll
         fun beforeAll() {
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
                     clock = Clock.systemDefaultZone(),
                 )
             }

             client = RestClient.withDefaultResponseHandler(
                 config = ClientConfig(scope = "meldekort-backend"),
                 tokenProvider = ClientCredentialsTokenProvider,
             )
         }

         @JvmStatic
         @AfterAll
         fun afterAll() {
             embeddedServer.stop()
         }
     }
 }