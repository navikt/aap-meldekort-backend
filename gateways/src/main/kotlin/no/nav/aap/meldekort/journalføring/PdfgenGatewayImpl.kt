package no.nav.aap.meldekort.journalføring

import java.net.URI
import java.time.Instant
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.journalføring.PdfgenGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.utfylling.Utfylling
import java.time.DayOfWeek
import java.time.ZoneId

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
        meldekort: no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort,
        utfylling: Utfylling
    ): ByteArray {
        val pdf = httpClient.post(
            uri, PostRequest(
                body = mapOf(
                    "ident" to ident.asString,
                    "navn" to "Test Testesen" /* TODO: pdl? token? */,
                    "sendtInnDato" to mottatt.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
                    "sendtInnUtc" to mottatt.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
                    "sammenlagtArbeidIPerioden" to utfylling.svar.timerArbeidet.sumOf { it.timer ?: 0.0 },
                    "harGittRiktigeOpplysninger" to utfylling.svar.svarerDuSant,
                    "meldeperiode" to mapOf(
                        "fraOgMedDato" to meldekort.fom().toString(),
                        "tilOgMedDato" to meldekort.tom().toString()
                    ),
                    "innsendingsvindu" to mapOf(
                        "fraOgMedDato" to utfylling.periode.tom.plusDays(1),
                        "tilOgMedDato" to utfylling.periode.tom.plusDays(8)
                    ),
                    "meldekort" to mapOf(
                        "harDuArbeidet" to utfylling.svar.harDuJobbet,
                        "timerArbeidPerUkeIPerioden" to utfylling.svar.timerArbeidet
                            .groupBy { it.dato.with(DayOfWeek.MONDAY) }
                            .map { (mandag, dager) ->
                                val sisteDagIDenneUken = dager.maxOfOrNull { it.dato } ?: mandag.plusDays(6)

                                mapOf(
                                    "fraOgMedDato" to mandag,
                                    "tilOgMedDato" to sisteDagIDenneUken,
                                    "dager" to dager.map { dag ->
                                        mapOf(
                                            "dag" to dag.dato.toString(),
                                            "timerArbeid" to (dag.timer ?: 0.0)
                                        )
                                    }
                                )
                            }
                    )
                ),
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