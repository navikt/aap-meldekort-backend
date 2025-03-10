package no.nav.aap.meldekort.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class UtfyllingRepositoryPostgresTest {
    @Test
    fun `enkel read write`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val flyt = UtfyllingFlytNavn.AAP_FLYT
            val ident = Ident("0".repeat(11))
            val opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val referanse = UtfyllingReferanse.ny()

            val utfyllingInn1 = Utfylling(
                referanse = referanse,
                ident = ident,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 13)),
                flyt = flyt,
                aktivtSteg = flyt.steg.first(),
                svar = Svar(
                    svarerDuSant = null,
                    harDuJobbet = true,
                    timerArbeidet = listOf(
                        TimerArbeidet(LocalDate.of(2020, 1, 1), null),
                        TimerArbeidet(LocalDate.of(2020, 1, 2), 0.0),
                        TimerArbeidet(LocalDate.of(2020, 1, 3), 7.0),
                        TimerArbeidet(LocalDate.of(2020, 1, 4), 7.5),
                    ),
                    stemmerOpplysningene = false,
                ),
                opprettet = opprettet,
                sistEndret = opprettet,
                fagsak = FagsakReferanse(
                    system = FagsystemNavn.KELVIN,
                    nummer = Fagsaknummer("sak1234"),
                ),
            )

            repo.lagrUtfylling(utfyllingInn1)

            repo.lastÅpenUtfylling(utfyllingInn1.ident, utfyllingInn1.periode).also {
                assertEquals(utfyllingInn1, it)
            }

            repo.lastUtfylling(utfyllingInn1.ident, utfyllingInn1.referanse).also {
                assertEquals(utfyllingInn1, it)
            }

            val utfyllingInn2 = utfyllingInn1.copy(
                sistEndret = opprettet.plusMillis(100),
                svar = utfyllingInn1.svar.copy(harDuJobbet = false)
            )
            repo.lagrUtfylling(utfyllingInn2)

            repo.lastÅpenUtfylling(utfyllingInn1.ident, utfyllingInn1.periode).also {
                assertEquals(utfyllingInn2, it)
            }

            repo.lastUtfylling(utfyllingInn1.ident, utfyllingInn1.referanse).also {
                assertEquals(utfyllingInn2, it)
            }

            val endeligUtfylling = utfyllingInn2.copy(
                sistEndret = utfyllingInn2.sistEndret.plusMillis(300),
                aktivtSteg = utfyllingInn2.flyt.steg.last(),
            )
            assertTrue(endeligUtfylling.erAvsluttet)
            repo.lagrUtfylling(endeligUtfylling)

            repo.lastÅpenUtfylling(utfyllingInn1.ident, utfyllingInn1.periode).also {
                assertNull(it)
            }

            repo.lastUtfylling(utfyllingInn1.ident, utfyllingInn1.referanse).also {
                assertEquals(endeligUtfylling, it)
            }
        }
    }
}