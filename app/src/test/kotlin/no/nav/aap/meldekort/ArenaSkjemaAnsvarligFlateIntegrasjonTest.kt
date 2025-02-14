package no.nav.aap.meldekort

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.ArenaMeldekort.ArenaStatus.UBEHA
import no.nav.aap.arena.ArenaPerson
import no.nav.aap.arena.ArenaSkjemaFlate
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortType
import no.nav.aap.skjema.TimerArbeidet
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals


class ArenaSkjemaAnsvarligFlateIntegrasjonTest {
    init {
        registerRepositories()
    }

    @Test
    fun `korrigering dukker opp i liste med historiske meldekort`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val arenaClient = FakeArenaGateway()

            val arenaSkjemaFlate = ArenaSkjemaFlate.konstruer(connection, arenaGateway = arenaClient)
            val meldeperiode = Periode(
                LocalDate.parse("2024-11-11"),
                LocalDate.parse("2024-11-24")
            )

            val originalMeldekortId = MeldekortId(1684229709L)
            arenaClient.historiskeMeldekort = ArenaPerson(
                personId = 4940857,
                etternavn = "GRUNNSKOLE",
                fornavn = "KOMISK",
                maalformkode = "NO",
                meldeform = "EMELD",
                arenaMeldekortListe = listOf(
                    ArenaMeldekort(
                        meldekortId = originalMeldekortId,
                        kortType = ArenaGateway.KortType.MANUELL_ARENA,
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
                meldekortId = MeldekortId(1687337285),
                kortType = ArenaGateway.KortType.KORRIGERT_ELEKTRONISK,
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