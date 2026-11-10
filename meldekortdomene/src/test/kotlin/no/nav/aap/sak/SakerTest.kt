package no.nav.aap.sak

import no.nav.aap.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertNull
import kotlin.test.assertSame

class SakerTest {
    private val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7))

    private fun sak(periode: Periode): Sak = object : Sak {
        override val referanse = FagsakReferanse(FagsystemNavn.ARENA, Fagsaknummer("1234"))
        override val rettighetsperiode = periode
    }

    @Test
    fun `når seneste til-og-med dato for en sak er etter perioden vi ser på returnerer vi ingen saker`() {
        val saker = Saker(
            listOf(
                sak(Periode(periode.fom.minusMonths(1), periode.fom.minusDays(1))),
                sak(Periode(periode.fom.minusMonths(1), periode.tom.plusDays(1))),
                sak(Periode(periode.fom.minusMonths(1), LocalDate.MAX)),
            ),
        )
        assertNull(saker.finnSakSomSlutterIPeriode(periode))
    }

    @Test
    fun `velger seneste sluttdato blant saker som slutter i perioden`() {
        val tidligsteSak = sak(Periode(periode.fom, periode.fom.plusDays(1)))
        val senesteSak = sak(Periode(periode.fom.minusMonths(1), periode.tom))
        val sakUtenforPerioden = sak(Periode(periode.fom, periode.tom.plusDays(1)))
        val saker = Saker(listOf(tidligsteSak, senesteSak, sakUtenforPerioden))

        assertSame(senesteSak, saker.finnSakSomSlutterIPeriode(periode))
    }

    @Test
    fun `ingen saker gir ingen treff`() {
        assertNull(Saker(emptyList()).finnSakSomSlutterIPeriode(periode))
    }
}
