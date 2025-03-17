package no.nav.aap.meldekort.journalføring

import java.net.URI
import java.time.Instant
import no.nav.aap.Ident
import no.nav.aap.journalføring.PdfgenGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.utfylling.Utfylling
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

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
                    "sendtInnDato" to formaterDatoForFrontend(mottatt.atZone(ZoneId.of("Europe/Oslo")).toLocalDate()),
                    "meldekortid" to utfylling.referanse.asUuid.toString(),
                    "sammenlagtArbeidIPerioden" to formaterTimer(utfylling.svar.timerArbeidet.sumOf {
                        it.timer ?: 0.0
                    }),
                    "harGittRiktigeOpplysninger" to utfylling.svar.svarerDuSant,
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
                        "timerArbeidPerUkeIPerioden" to utfylling.svar.timerArbeidet
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
            "ingen respons fra pdfgen-meldekort"
        }
        check(pdf.sliceArray(0..3).contentEquals("%PDF".toByteArray())) {
            "Body fra dokgen mangler PDF-magic number '%PDF'. Html/json-feilmelding?"
        }
        return pdf
    }
}

fun formaterDatoForFrontend(date: LocalDate?): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("no-NO"))
    return date?.format(formatter) ?: ""
}

fun hentDagNavn(date: LocalDate): String {
    val dag = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("no-NO"))
    return dag.replaceFirstChar { it.uppercaseChar() }
}

fun formaterTimer(number: Double?): String {
    return when {
        number == null -> ""
        number % 1.0 == 0.0 -> number.toInt().toString()
        else -> number.toString()
    }
}

fun hentUkeNummerForDato(dato: LocalDate): String {
    val ukeFelter = WeekFields.of(Locale.forLanguageTag("no-NO"))
    return dato.get(ukeFelter.weekOfWeekBasedYear()).toString()
}

fun hentUkeNummerForPerioen(from: LocalDate?, to: LocalDate?): String {
    val ukeFelter = WeekFields.of(Locale.forLanguageTag("no-NO"))
    val uker = generateSequence(from) { it.plusDays(1) }
        .takeWhile { it <= to }
        .map { it.get(ukeFelter.weekOfWeekBasedYear()) }
        .distinct()
        .toList()

    return when (uker.size) {
        1 -> "Uke ${uker.first()}"
        2 -> "Uke ${uker.joinToString(" og ")}"
        else -> "Uke ${uker.dropLast(1).joinToString(", ")} og ${uker.last()}"
    }
}
