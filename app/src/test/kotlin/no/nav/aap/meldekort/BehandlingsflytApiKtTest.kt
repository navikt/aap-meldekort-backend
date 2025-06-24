package no.nav.aap.meldekort

import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.meldekort.test.FakeAzure
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.meldekort.test.FakeTokenX
import no.nav.aap.meldekort.test.port
import no.nav.aap.postgresRepositoryRegistry
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.io.InputStream
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.test.Test

class BehandlingsflytApiKtTest {

    @Test
    fun `sjekk at data settes korrekt i databasen ved melding fra behandlingsflyt`() {
//        "/api/behandlingsflyt/sak/meldeperioder"
        val rettighetsperiode = Periode(fom = LocalDate.of(2025, 3, 3), tom = LocalDate.of(2025, 4, 27))
        // TODO kan dette bygges ut fra rettighetsperioden?
        val meldeperioder = listOf(
            Periode(fom = LocalDate.of(2025, 3, 3), tom = LocalDate.of(2025, 3, 16)),
            Periode(fom = LocalDate.of(2025, 3, 17), tom = LocalDate.of(2025, 3, 30)),
            Periode(fom = LocalDate.of(2025, 3, 31  ), tom = LocalDate.of(2025, 4, 13)),
            Periode(fom = LocalDate.of(2025, 4, 14), tom = LocalDate.of(2025, 4, 27)),
        )

        val meldeplikt = listOf(
            Periode(fom = LocalDate.of(2025, 3, 31), tom = LocalDate.of(2025, 4, 7)),
            Periode(fom = LocalDate.of(2025, 4, 14), tom = LocalDate.of(2025, 4, 21)),
            Periode(fom = LocalDate.of(2025, 4, 28), tom = LocalDate.of(2025, 5, 5)),
        )

        val meldedata = MeldeperioderV0(
            sakenGjelderFor = rettighetsperiode,
            saksnummer = "SAKSNUMMER",
            identer = listOf("1234678910"),
            meldeperioder = meldeperioder,
            meldeplikt = meldeplikt
        )
        post<MeldeperioderV0>(meldedata)
    }



     companion object {
         private lateinit var embeddedServer: EmbeddedServer<*, *>
         lateinit var client: RestClient<InputStream>
         private val fakeServers = FakeServers()

         val dataSource = createTestcontainerPostgresDataSource(prometheus)

         val baseUrl: String
             get() = "http://localhost:${embeddedServer.port()}"

         val tokenSchmoken:OidcToken? = null
         fun getToken(): OidcToken {
            val mahClient = RestClient(
                config = ClientConfig(scope = "behandlingsflyt"),
                tokenProvider = NoTokenTokenProvider(),
                responseHandler = DefaultResponseHandler()
            )
             return tokenSchmoken ?: OidcToken(
                 mahClient.post<String, FakeServers.TestToken>(
                     URI(requiredConfigForKey("azure.openid.config.token.endpoint")),
                     PostRequest("grant_type=client_credentials"),
                 )!!.access_token
             )
         }

         inline fun <reified T> post(meldedata: MeldeperioderV0): T? {
             return client.post<_, T>(
                 URI("${baseUrl}/api/behandlingsflyt/sak/meldeperioder"), PostRequest(
                     body = meldedata,
                     currentToken = getToken()
                 )
             )
         }

         @JvmStatic
         @BeforeAll
         fun beforeAll() {
             fakeServers.start()

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
                 config = ClientConfig(),
                 tokenProvider = object : TokenProvider {},
             )
         }

         @JvmStatic
         @AfterAll
         fun afterAll() {
             embeddedServer.stop(0L, 0L)
             fakeServers.close()
         }
     }
 }