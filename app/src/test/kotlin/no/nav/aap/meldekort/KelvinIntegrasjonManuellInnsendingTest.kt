package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.Periode
import org.junit.jupiter.api.AutoClose
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
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.meldekort.FraværSvarDto.GJENNOMFØRT_AVTALT_AKTIVITET
import no.nav.aap.meldekort.journalføring.DokarkivGatewayImpl
import no.nav.aap.meldekort.journalføring.PdfgenGatewayImpl
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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * ```
 * 2024
 *         July                     August                  September
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *   1  2  3  4  5  6  7                1  2  3  4                         1
 *   8  9 10 11 12 13 14       5  6  7  8  9 10 11       2  3  4  5  6  7  8
 *  15 16 17 18 19 20 21      12 13 14 15 16 17 18       9 10 11 12 13 14 15
 *  22 23 24 25 26 27 28      19 20 21 22 23 24 25      16 17 18 19 20 21 22
 *  29 30 31                  26 27 28 29 30 31         23 24 25 26 27 28 29
 *                                                      30
 *
 *
 *       October                   November                  December
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *      1  2  3  4  5  6                   1  2  3                         1
 *   7  8  9 10 11 12 13       4  5  6  7  8  9 10       2  3  4  5  6  7  8
 *  14 15 16 17 18 19 20      11 12 13 14 15 16 17       9 10 11 12 13 14 15
 *  21 22 23 24 25 26 27      18 19 20 21 22 23 24      16 17 18 19 20 21 22
 *  28 29 30 31               25 26 27 28 29 30         23 24 25 26 27 28 29
 *                                                      30 31
 *
 *  2025
 *
 *       January                   February                   March
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *         1  2  3  4  5                      1  2                      1  2
 *  [6] 7  8  9 10 11 12       3  4  5  6  7  8  9       3  4  5  6  7  8  9
 *  13 14 15 16 17 18 19      10 11 12 13 14 15 16      10 11 12 13 14 15 16
 *  20 21 22 23 24 25 26      17 18 19 20 21 22 23      17 18 19 20 21 22 23
 *  27 28 29 30 31            24 25 26 27 28            24 25 26 27 28 29 30
 *                                                      31
 *
 *
 *        April                      May                       June
 *  Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
 *      1  2  3  4  5  6                1  2  3  4                         1
 *   7  8  9 10 11 12 13       5  6  7  8  9 10 11       2  3  4  5  6  7  8
 *  14 15 16 17 18 19 20      12 13 14 15 16 17 18       9 10 11 12 13 14 15
 *  21 22 23 24 25 26 27      19 20 21 22 23 24 25      16 17 18 19 20 21 22
 *  28 29 30                  26 27 28 29 30 31         23 24 25 26 27 28 29
 *                                                      30
 * ```
 */


class KelvinIntegrasjonManuellInnsendingTest {
    @AutoClose
    val app = AppInstance()

    @Test
    fun `Kunne sende inn digitaliserte meldekort fra behandlingsflyt imellom elektroniske innsendinger`() {
        val idag = LocalDate.of(2025, 12, 1)
        app.idag = idag

        val fnr = fødselsnummerGenerator.next()
        val rettighetsperiode = Periode(17 november 2025, idag.plusWeeks(51))
        app.kelvinSak(
            fnr,
            rettighetsperiode = rettighetsperiode,
            opplysningsbehov = listOf(Periode(17 november 2025, idag.plusWeeks(51)))
        )

        app.fyllInnTimer(
            fnr,
            opplysningerOm = Periode(17 november 2025, 30 november 2025),
            sakStart = rettighetsperiode.fom
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-01", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-14", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-12-22", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        app.idag = LocalDate.of(2025, 12, 15)
        app.fyllInnTimer(
            fnr,
            opplysningerOm = Periode(1 desember 2025, 14 desember 2025),
            sakStart = rettighetsperiode.fom
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-28", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-05", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        app.idag = LocalDate.of(2025, 12, 29)

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-15", it, "/manglerOpplysninger/fom")
            assertEqualsAt("2025-12-28", it, "/manglerOpplysninger/tom")
            assertEqualsAt("2025-12-15", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-28", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-05", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        val utfyllingReferanse = app.fyllInnTimerFraBehandlingsflyt(
            fnr, rettighetsperiode, Periode(15 desember 2025, 28 desember 2025)
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2026-01-11", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2026-01-12", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-19", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
        val utfylling = app.hentUtfyllinger(fnr, utfyllingReferanse)
        assertEquals(utfylling?.erDigitalisert, true)
    }
}