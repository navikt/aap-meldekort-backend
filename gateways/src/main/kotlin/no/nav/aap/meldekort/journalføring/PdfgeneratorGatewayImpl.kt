package no.nav.aap.meldekort.journalføring

import no.nav.aap.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.journalføring.PdfgeneratorGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.prometheus
import no.nav.aap.utfylling.Utfylling
import java.net.URI
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

object PdfgeneratorGatewayImpl : PdfgeneratorGateway {
    private val baseUrl = requiredConfigForKey("pdfgenerator.url")
    private val uri = URI("$baseUrl/api/v1/genpdf/innbygger/meldekort")

    private val httpClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = object : TokenProvider {},
        prometheus = prometheus
    )

    override fun genererPdf(
        ident: Ident,
        mottatt: Instant,
        meldekort: Meldekort,
        utfylling: Utfylling,
        harBrukerVedtakIKelvin: Boolean
    ): ByteArray {
        val pdf = httpClient.post(
            uri, PostRequest(
                body = mapOf(
                    "ident" to ident.asString,
                    "sendtInnDato" to formaterDatoForFrontend(mottatt.atZone(ZoneId.of("Europe/Oslo")).toLocalDate()),
                    "meldekortid" to utfylling.referanse.asUuid.toString(),
                    "sammenlagtArbeidIPerioden" to formaterTimer(utfylling.svar.aktivitetsInformasjon.sumOf {
                        it.timer ?: 0.0
                    }),
                    "harGittRiktigeOpplysninger" to utfylling.svar.svarerDuSant,
                    "harBrukerVedtakIKelvin" to harBrukerVedtakIKelvin,
                    "meldeperiode" to mapOf(
                        "fraOgMedDato" to formaterDatoForFrontend(meldekort.fom()),
                        "tilOgMedDato" to formaterDatoForFrontend(meldekort.tom()),
                        "uker" to hentUkeNummerForPerioen(meldekort.fom(), meldekort.tom()),
                    ),
                    "innsendingsvindu" to mapOf(
                        "fraOgMedDato" to formaterDatoForFrontend(utfylling.periode.tom.plusDays(1)),
                        "tilOgMedDato" to formaterDatoForFrontend(utfylling.periode.tom.plusDays(8))
                    ),
                    "meldekort" to mapOf(
                        "harDuArbeidet" to utfylling.svar.harDuJobbet,
                        "timerArbeidPerUkeIPerioden" to utfylling.svar.aktivitetsInformasjon
                            .filter { it.timer != null }
                            .groupBy { it.dato.with(DayOfWeek.MONDAY) }
                            .map { (mandag, dager) ->
                                val sisteDagIDenneUken = dager.maxOfOrNull { it.dato } ?: mandag.plusDays(6)

                                mapOf(
                                    "fraOgMedDato" to formaterDatoForFrontend(mandag),
                                    "tilOgMedDato" to formaterDatoForFrontend(sisteDagIDenneUken),
                                    "ukenummer" to hentUkeNummerForDato(mandag),
                                    "dager" to dager.map { dag ->
                                        mapOf(
                                            "dag" to hentDagNavn(dag.dato),
                                            "timerArbeid" to formaterTimer(dag.timer)
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
            "ingen respons fra pdfgenerator"
        }
        check(pdf.sliceArray(0..3).contentEquals("%PDF".toByteArray())) {
            "Body fra pdfgenerator mangler PDF-magic number '%PDF'. Html/json-feilmelding?"
        }
        return pdf
    }
}

