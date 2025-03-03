package no.nav.aap.meldekort.journalføring

import java.net.URI
import java.time.Instant
import no.nav.aap.Ident
import no.nav.aap.journalføring.PdfgenGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider

object PdfgenGatewayImpl : PdfgenGateway {
    private val baseUrl = requiredConfigForKey("pdfgen.url")
    private val uri = URI("$baseUrl/api/v1/genpdf/meldekort-backend/meldekort")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(),
        object : TokenProvider {},
    )

    override fun genererPdf(
        ident: Ident,
        mottatt: Instant,
        meldekort: no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
    ): ByteArray {
        val pdf = httpClient.get<ByteArray>(
            uri, GetRequest(
                additionalHeaders = listOf(
                    Header("accept", "application/pdf"),
                    Header("content-type", "application/json")
                )
            )
        ) { responseBody, _ ->
            responseBody.readAllBytes()
        }
        requireNotNull(pdf) {
            "ingen respons fra pdfgen-meldekort"
        }
        check(pdf.sliceArray(0..3).contentEquals("%PDF".toByteArray())) {
            "Body fra dokgen mangler PDF-magic number '%PDF'. Html/json-feilmelding?"
        }
        return pdf
    }
}