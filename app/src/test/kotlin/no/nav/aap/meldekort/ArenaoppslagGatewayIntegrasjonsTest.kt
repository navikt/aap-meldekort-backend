package no.nav.aap.meldekort

import io.ktor.http.HttpStatusCode
import no.nav.aap.Periode
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.json.DeserializationException
import no.nav.aap.meldekort.saker.ArenaoppslagGatewayImpl
import no.nav.aap.meldekort.test.FakeArenaoppslag
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArenaoppslagGatewayIntegrasjonsTest {
    private val idag = LocalDate.of(2026, 1, 1)

    private fun vedtak(sakId: Int = 1234) = ArenaVedtak(
        sakId = sakId,
        statusKode = "IVERK",
        vedtaktypeKode = "O",
        fraOgMed = idag,
        tilDato = idag.plusMonths(1),
        rettighetkode = "AAP",
        vedtakId = 1,
        utfallkode = "JA",
        aktivitetsfaseKode = "AU"
    )

    @Test
    fun `henter historiske og fremtidige vedtak`() {
        val ident = fødselsnummerGenerator.next()
        val vedtak = listOf(
            vedtak(1234).copy(fraOgMed = idag.minusYears(1), tilDato = idag.minusMonths(1), statusKode = "AVSLU"),
            vedtak(5678).copy(fraOgMed = idag.plusMonths(1), tilDato = idag.plusMonths(2)),
        )
        FakeArenaoppslag.settVedtak(ident, vedtak)

        val saker = ArenaoppslagGatewayImpl.hentSaker(ident)

        assertEquals(VedtakForPersonRequest(ident.asString), FakeArenaoppslag.forespørsler[ident.asString])
        vedtak.forEach {
            val referanse = FagsakReferanse(FagsystemNavn.ARENA, Fagsaknummer(it.sakId.toString()))
            val sak = assertNotNull(saker.finnSak(referanse))
            assertEquals(Periode(assertNotNull(it.fraOgMed), assertNotNull(it.tilDato)), sak.rettighetsperiode)
        }
        assertNull(saker.finnSakForDagen(idag))
    }

    @ParameterizedTest
    @ValueSource(strings = ["O", "E", "G"])
    fun `innvilgede vedtakstyper gir sak`(type: String) {
        val ident = fødselsnummerGenerator.next()
        FakeArenaoppslag.settVedtak(ident, listOf(vedtak().copy(vedtaktypeKode = type)))

        assertNotNull(ArenaoppslagGatewayImpl.hentSaker(ident).finnSakForDagen(idag))
    }

    @Test
    fun `ignorerer andre ytelser, avslag, uferdige, stans og ugyldige perioder`() {
        val ident = fødselsnummerGenerator.next()
        val grunnlag = vedtak()
        FakeArenaoppslag.settVedtak(
            ident,
            listOf(
                grunnlag.copy(rettighetkode = "DAGP"),
                grunnlag.copy(utfallkode = "NEI"),
                grunnlag.copy(utfallkode = null),
                grunnlag.copy(statusKode = "MOTAT"),
                grunnlag.copy(statusKode = "GODKJ"),
                grunnlag.copy(vedtaktypeKode = "S"),
                grunnlag.copy(vedtaktypeKode = "K"),
                grunnlag.copy(vedtaktypeKode = null),
                grunnlag.copy(fraOgMed = null),
                grunnlag.copy(tilDato = idag.minusDays(1)),
            ),
        )

        assertNull(ArenaoppslagGatewayImpl.hentSaker(ident).finnSakForDagen(idag))
    }

    @Test
    fun `ukjent person og tom vedtaksliste gir ingen saker`() {
        val ukjentIdent = fødselsnummerGenerator.next()
        val identUtenVedtak = fødselsnummerGenerator.next()
        FakeArenaoppslag.settVedtak(identUtenVedtak, emptyList())

        assertNull(ArenaoppslagGatewayImpl.hentSaker(ukjentIdent).finnSakForDagen(idag))
        assertNull(ArenaoppslagGatewayImpl.hentSaker(identUtenVedtak).finnSakForDagen(idag))
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `autentiseringsfeil blir ikke til tomme saker`(status: Int) {
        val ident = fødselsnummerGenerator.next()
        FakeArenaoppslag.settRespons(ident, HttpStatusCode.fromValue(status), "Access denied")

        assertFailsWith<ManglerTilgangException> {
            ArenaoppslagGatewayImpl.hentSaker(ident)
        }
    }

    @Test
    fun `ugyldig respons blir ikke til tomme saker`() {
        val ident = fødselsnummerGenerator.next()
        FakeArenaoppslag.settRespons(ident, HttpStatusCode.OK, """{"unexpected":"response"}""")

        assertFailsWith<DeserializationException> {
            ArenaoppslagGatewayImpl.hentSaker(ident)
        }
    }

    @Test
    fun `manglende respons blir ikke til tomme saker`() {
        val ident = fødselsnummerGenerator.next()
        FakeArenaoppslag.settRespons(ident, HttpStatusCode.NoContent, "")

        assertFailsWith<IllegalArgumentException> {
            ArenaoppslagGatewayImpl.hentSaker(ident)
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun startFakeServers() {
            FakeServers.start()
        }
    }
}
