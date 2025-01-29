package no.nav.aap.meldekort

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.meldekort.arena.ArenaClient
import no.nav.aap.meldekort.arena.ArenaMeldekort
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.UBEHA
import no.nav.aap.meldekort.arena.ArenaPerson
import no.nav.aap.meldekort.arena.ArenaSkjemaFlate
import no.nav.aap.meldekort.arena.MeldekortType
import no.nav.aap.meldekort.arena.TimerArbeidet
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals


class ArenaSkjemaFlateIntegrasjonTest {
    init {
        registerRepositories()
    }

    @Test
    fun `korrigering dukker opp i liste med historiske meldekort`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val arenaClient = FakeArenaClient()

            val arenaSkjemaFlate = ArenaSkjemaFlate.konstruer(connection, arenaClient = arenaClient)
            val meldeperiode = Periode(
                LocalDate.parse("2024-11-11"),
                LocalDate.parse("2024-11-24")
            )

            val originalMeldekortId = 1684229709L
            arenaClient.historiskeMeldekort = ArenaPerson(
                personId = 4940857,
                etternavn = "GRUNNSKOLE",
                fornavn = "KOMISK",
                maalformkode = "NO",
                meldeform = "EMELD",
                arenaMeldekortListe = listOf(
                    ArenaMeldekort(
                        meldekortId = originalMeldekortId,
                        kortType = ArenaClient.KortType.MANUELL_ARENA,
                        meldeperiode = "202446",
                        fraDato = meldeperiode.fom,
                        tilDato = meldeperiode.tom,
                        hoyesteMeldegruppe = "ATTF",
                        beregningstatus = ArenaMeldekort.ArenaStatus.FERDI,
                        forskudd = false,
                        mottattDato = LocalDate.parse("2024-12-12"),
                        bruttoBelop = 8831.0,
                    )
                )
            )
            arenaClient.korrigertMeldekort = ArenaMeldekort(
                meldekortId = 1687337285,
                kortType = ArenaClient.KortType.KORRIGERT_ELEKTRONISK,
                meldeperiode = "202446",
                fraDato = meldeperiode.fom,
                tilDato = meldeperiode.tom,
                hoyesteMeldegruppe = "ATTF",
                beregningstatus = UBEHA,
                forskudd = false,
                mottattDato = LocalDate.parse("2024-12-12"),
                bruttoBelop = 0.0,
            )


            val timerArbeidet = listOf(
                TimerArbeidet(10.0, meldeperiode.tom)
            )
            val innloggetBruker = InnloggetBruker(Ident(""), "")
            arenaSkjemaFlate.korrigerMeldekort(
                innloggetBruker,
                originalMeldekortId = originalMeldekortId,
                timerArbeidet = timerArbeidet,
            )

            val historiskeMeldekort = arenaSkjemaFlate.historiskeMeldekortDetaljer(
                innloggetBruker = innloggetBruker,
                meldeperiode = meldeperiode,
            )

            val (korrigering, førstegangsRegistrering) =
                historiskeMeldekort.partition { it.meldekort.type == MeldekortType.KORRIGERING }

            assertEquals(timerArbeidet, korrigering.single().timerArbeidet)
            assertNull(førstegangsRegistrering.single().timerArbeidet)


            assertThrows<Exception> {
                arenaSkjemaFlate.korrigerMeldekort(
                    innloggetBruker,
                    originalMeldekortId = originalMeldekortId,
                    timerArbeidet = listOf(TimerArbeidet(11.0, meldeperiode.tom))
                )
            }

        }
    }
}