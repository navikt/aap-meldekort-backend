package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.OnBehalfOfTokenProvider
import no.nav.aap.meldekort.InnloggetBruker
import org.slf4j.MDC
import java.net.URI
import java.util.*

class ArenaClient(
    private val meldekortserviceUrl: String,
    meldekortserviceScope: String,
    private val meldekortkontrollUrl: String,
    meldekortkontrollScope: String,
): Arena {
    private val meldekortserviceClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = meldekortserviceScope),
        tokenProvider = OnBehalfOfTokenProvider()
    )
    private val meldekortkontrollClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = meldekortkontrollScope),
        tokenProvider = OnBehalfOfTokenProvider()
    )

    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<Arena.Meldegruppe> {
        return requireNotNull(getMeldekortservice("/v2/meldegrupper", innloggetBruker))
    }

    override fun meldekort(innloggetBruker: InnloggetBruker): Arena.Person? {
        return getMeldekortservice("/v2/meldekort", innloggetBruker)
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): Arena.Person {
        return requireNotNull(getMeldekortservice("/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder", innloggetBruker))
    }

    override fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: Long): Arena.Meldekortdetaljer {
        // burde bruke URI builder av noe slag, siden vi ikke har kontroll på hvor meldekortId kommer fra
        return requireNotNull(getMeldekortservice("/v2/meldekortdetaljer?meldekortId=$meldekortId", innloggetBruker))
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long {
        // burde bruke URI builder av noe slag, siden vi ikke har kontroll på hvor meldekortId kommer fra
        return requireNotNull(getMeldekortservice("/v2/korrigertMeldekort?meldekortId=$meldekortId", innloggetBruker))
    }

    override fun sendInn(innloggetBruker: InnloggetBruker, request: Arena.MeldekortkontrollRequest): Arena.MeldekortkontrollResponse {
        return requireNotNull(meldekortkontrollClient.post(URI("$meldekortkontrollUrl/api/v1/kontroll"), PostRequest(
            body = request
        )))
    }

    private inline fun <reified Response> getMeldekortservice(path: String, innloggetBruker: InnloggetBruker): Response? {
        return meldekortserviceClient.get(URI(meldekortserviceUrl + path), getRequest(innloggetBruker))
    }
    private fun getRequest(innloggetBruker: InnloggetBruker): GetRequest {
        return GetRequest(
            currentToken = OidcToken(innloggetBruker.token),
            additionalHeaders = listOf(
                Header("ident", innloggetBruker.ident),
                Header("x-request-id", MDC.get("callId") ?: "aap-meldekort-${UUID.randomUUID()}")
            ),
        )
    }
}