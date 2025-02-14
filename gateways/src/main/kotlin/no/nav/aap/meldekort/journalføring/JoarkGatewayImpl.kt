package no.nav.aap.meldekort.journalføring

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.ConflictHttpResponseException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.journalføring.JoarkClient
import java.net.URI

object JoarkGatewayImpl : JoarkClient {
    private val joarkUrl = requiredConfigForKey("joark.url")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("joark.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    override fun oppdater(
        journalpost: JoarkClient.Journalpost,
        forsøkFerdigstill: Boolean
    ): JoarkClient.JournalpostResponse {
        return try {
            httpClient.post<_, JoarkClient.JournalpostResponse>(
                uri = URI("$joarkUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=$forsøkFerdigstill"),
                request = PostRequest(
                    body = journalpost,
                    additionalHeaders = listOf(Header("accept", "application/json")),
                ),
            )!!
        } catch (e: ConflictHttpResponseException) {
            DefaultJsonMapper.fromJson<JoarkClient.JournalpostResponse>(e.body!!)
        }

    }
}