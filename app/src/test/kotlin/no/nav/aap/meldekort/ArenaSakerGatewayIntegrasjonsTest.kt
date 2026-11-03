package no.nav.aap.meldekort

import no.nav.aap.Periode
import no.nav.aap.meldekort.saker.AapGatewayImpl
import no.nav.aap.meldekort.test.FakeAapApi
import no.nav.aap.meldekort.test.FakeServers
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.FagsystemNavn
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArenaSakerGatewayIntegrasjonsTest {
    @Test
    fun `henter arena-saksnummer og perioder over HTTP uten datofiltrering`() {
        val ident = fødselsnummerGenerator.next()
        val perioder = listOf(
            Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)),
            Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
        )
        val arenaSaker = perioder.map { periode ->
            FakeAapApi.FakeSak(
                FagsakReferanse(FagsystemNavn.ARENA, saksnummerGenerator.next()),
                periode,
            ).also { FakeAapApi.upsert(ident, it) }
        }
        val kelvinReferanse = FagsakReferanse(FagsystemNavn.KELVIN, saksnummerGenerator.next())
        FakeAapApi.upsert(ident, FakeAapApi.FakeSak(kelvinReferanse, perioder.first()))

        val saker = AapGatewayImpl.hentSakerFraArena(ident)

        arenaSaker.forEach { forventet ->
            val sak = assertNotNull(saker.finnSak(forventet.referanse))
            assertEquals(forventet.referanse, sak.referanse)
            assertEquals(forventet.rettighetsperiode, sak.rettighetsperiode)
        }
        assertNull(saker.finnSak(kelvinReferanse))
    }

    @Test
    fun `ukjent ident gir ingen arena-saker`() {
        val saker = AapGatewayImpl.hentSakerFraArena(fødselsnummerGenerator.next())

        assertNull(saker.finnSakForDagen(LocalDate.of(2026, 1, 1)))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun startFakeServers() {
            FakeServers.start()
        }
    }
}
