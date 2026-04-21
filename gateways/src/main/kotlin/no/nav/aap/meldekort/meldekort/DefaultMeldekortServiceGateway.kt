package no.nav.aap.meldekort.meldekort

import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.MeldekortServiceGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.prometheus
import java.net.URI

object DefaultMeldekortServiceGateway : MeldekortServiceGateway {

    private val meldekortUri = URI("${requiredConfigForKey("meldekortservice.url")}/v2/meldekort")
    private val historiskeMeldekortUri = URI("${requiredConfigForKey("meldekortservice.url")}/v2/historiskemeldekort")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("meldekortservice.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus,
    )

    override fun hentMeldekort(fnr: String): List<ArenaMeldekort>? {
        val response = httpClient.get<ArenaMeldekortResponse?>(
            uri = meldekortUri,
            request = GetRequest(
                additionalHeaders = listOf(
                    Header("accept", "application/json"),
                    Header("ident", fnr),
                ),
            ),
        ) ?: return null
        return response.meldekortListe ?: emptyList()
    }

    override fun hentHistoriskeMeldekort(fnr: String, antallMeldeperioder: Int): List<ArenaMeldekort>? {
        val response = httpClient.get<ArenaMeldekortResponse?>(
            uri = historiskeMeldekortUri.withQuery("antallMeldeperioder=$antallMeldeperioder"),
            request = GetRequest(
                additionalHeaders = listOf(
                    Header("accept", "application/json"),
                    Header("ident", fnr),
                ),
            ),
        ) ?: return null
        return response.meldekortListe ?: emptyList()
    }

    private fun URI.withQuery(query: String) = URI(scheme, authority, path, query, null)
}