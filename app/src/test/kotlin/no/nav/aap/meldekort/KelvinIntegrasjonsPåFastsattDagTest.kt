package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.Periode
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDate

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


class KelvinIntegrasjonsPåFastsattDagTest {
    @AutoClose
    val app = AppInstance()

    @Test
    fun `ikke vedtak, på dagen`() {
        val idag = 6 januar 2025
        app.idag = idag
        val fnr = fødselsnummerGenerator.next()
        val meldeperiode1 = Periode(6 januar 2025, 19 januar 2025)
        val meldevindu1 = Periode(20 januar 2025, 27 januar 2025)
        app.kelvinSak(fnr, rettighetsperiode = Periode(idag, idag.plusWeeks(52)))
        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `ikke vedtak, en uke etter søknad`() {
        val idag = 6 januar 2025
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(fnr, rettighetsperiode = Periode(idag.minusWeeks(1), idag.plusWeeks(51)))
        val meldeperiode1 = Periode(30 desember 2024, 12 januar 2025)
        val meldevindu1 = Periode(13 januar 2025, 20 januar 2025)
        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `ikke vedtak, tre uker etter søknad`() {
        val idag = 6 januar 2025
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(fnr, rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(51)))
        val meldeperiode1 = Periode(16 desember 2024, 29 desember 2024)
        val meldevindu1 = Periode(30 desember 2024, 6 januar 2025)
        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(meldeperiode1, it, "/manglerOpplysninger")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `med vedtak, tre uker etter søknad`() {
        val idag = 6 januar 2025
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(
            fnr,
            rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(49)),
            opplysningsbehov = listOf(Periode(16 desember 2024, idag.plusWeeks(49)))
        )
        val meldeperiode1 = Periode(16 desember 2024, 29 desember 2024)
        val meldevindu1 = Periode(30 desember 2024, 6 januar 2025)
        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(meldeperiode1, it, "/manglerOpplysninger")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt(meldeperiode1, it, "/nesteMeldeperiode/meldeperiode")
            assertEqualsAt(meldevindu1, it, "/nesteMeldeperiode/innsendingsvindu")
        }
    }

    @Test
    fun `med vedtak, en uke etter søknad med opplysningsbehov i dag`() {
        val idag = 6 januar 2025
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(
            fnr,
            rettighetsperiode = Periode(16 desember 2024, idag.plusWeeks(51)),
            opplysningsbehov = listOf(Periode(16 desember 2024, idag.plusWeeks(51)))
        )
        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt("2024-12-16", it, "/manglerOpplysninger/fom")
            assertEqualsAt("2024-12-29", it, "/manglerOpplysninger/tom")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2024-12-16", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2024-12-29", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2024-12-30", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-01-06", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
    }

    @Test
    fun `kommende meldeperiode for helligdagsunntak`() {
        val idag = LocalDate.of(2025, 12, 8)
        app.idag = idag

        val fnr = fødselsnummerGenerator.next()
        val rettighetsperiode = Periode(29 november 2025, idag.plusWeeks(51))
        app.kelvinSak(
            fnr,
            rettighetsperiode = rettighetsperiode,
            opplysningsbehov = listOf(Periode(29 november 2025, idag.plusWeeks(51)))
        )

        app.fyllInnTimer(
            fnr,
            opplysningerOm = Periode(24 november 2025, 7 desember 2025),
            sakStart = rettighetsperiode.fom
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-08", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-21", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-17", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        app.idag = LocalDate.of(2025, 12, 17)

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt("2025-12-08", it, "/manglerOpplysninger/fom")
            assertEqualsAt("2025-12-21", it, "/manglerOpplysninger/tom")
            assertEqualsAt(1, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-08", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2025-12-21", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2025-12-17", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }

        app.fyllInnTimer(
            fnr,
            opplysningerOm = Periode(8 desember 2025, 21 desember 2025),
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-22", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2026-01-04", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2026-01-05", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-12", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
    }

    @Test
    fun `uendret oppførsel for kommende meldeperioder som går over helligdagsunntak`() {
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

        app.fyllInnTimer(
            fnr,
            opplysningerOm = Periode(15 desember 2025, 28 desember 2025),
            sakStart = rettighetsperiode.fom
        )

        app.get<JsonNode>(fnr, "/api/meldeperiode/kommende")!!.also {
            assertEqualsAt(null, it, "/manglerOpplysninger")
            assertEqualsAt(0, it, "/antallUbesvarteMeldeperioder")
            assertEqualsAt("2025-12-29", it, "/nesteMeldeperiode/meldeperiode/fom")
            assertEqualsAt("2026-01-11", it, "/nesteMeldeperiode/meldeperiode/tom")
            assertEqualsAt("2026-01-12", it, "/nesteMeldeperiode/innsendingsvindu/fom")
            assertEqualsAt("2026-01-19", it, "/nesteMeldeperiode/innsendingsvindu/tom")
        }
    }
}
