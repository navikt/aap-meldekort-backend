package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.arena.MeldekortType.VANLIG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class UtfyllingRepositoryPostgresTest {
    private val flyt = UtfyllingFlyt(BekreftSvarerÆrligSteg)

    @Test
    fun lastSkjema() {
        InitTestDatabase.dataSource.transaction {
            val repo = UtfyllingRepositoryPostgres(it)

            assertNull(
                repo.last(
                    nextIdent(),
                    MeldekortId(0),
                    UtfyllingFlyt()
                )
            )
        }
    }

    @Test
    fun `lagre og hent skjema`() {
        InitTestDatabase.dataSource.transaction {
            val ident = nextIdent()

            val repo = UtfyllingRepositoryPostgres(it)
            val meldekortRepo = MeldekortRepositoryPostgres(it)

            val utfylling = Utfylling(
                steg = BekreftSvarerÆrligSteg,
                flyt = flyt,
                skjema = Skjema(
                    tilstand = SkjemaTilstand.SENDT_ARENA,
                    meldekortId = MeldekortId(0),
                    ident = ident,
                    meldeperiode = Periode(
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2022, 1, 1)
                    ),
                    referanse = UUID.randomUUID(),
                    sendtInn = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                    payload = InnsendingPayload(
                        svarerDuSant = true,
                        harDuJobbet = false,
                        timerArbeidet = listOf(
                            TimerArbeidet(timer = 7.0, dato = LocalDate.of(2020, 1, 14)),
                            TimerArbeidet(timer = 7.5, dato = LocalDate.of(2020, 1, 15)),
                            TimerArbeidet(timer = 0.0, dato = LocalDate.of(2020, 1, 16)),
                            TimerArbeidet(timer = 0.0, dato = LocalDate.of(2020, 1, 17)),
                            TimerArbeidet(timer = null, dato = LocalDate.of(2020, 1, 18)),
                        ),
                        stemmerOpplysningene = null
                    )
                )
            )


            meldekortRepo.upsert(
                ident, KommendeMeldekort(
                    meldekortId = MeldekortId(0),
                    type = VANLIG,
                    periode = Periode(
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2022, 1, 1)
                    ),
                    kanKorrigeres = true
                )
            )
            repo.lagrUtfylling(utfylling)

            assertEquals(utfylling, repo.last(ident, MeldekortId(0), flyt))

            val endretSkjema =
                utfylling.copy(skjema = utfylling.skjema.copy(tilstand = SkjemaTilstand.FORSØKER_Å_SENDE_TIL_ARENA))

            Thread.sleep(2)

            repo.lagrUtfylling(endretSkjema)

            assertEquals(endretSkjema, repo.last(ident, MeldekortId(0), flyt))
        }
    }
}