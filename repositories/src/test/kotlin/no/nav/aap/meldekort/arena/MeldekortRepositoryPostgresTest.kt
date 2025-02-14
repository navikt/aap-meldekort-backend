package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.Periode
import no.nav.aap.arena.MeldekortType.KORRIGERING
import no.nav.aap.arena.MeldekortType.VANLIG
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import no.nav.aap.arena.HistoriskMeldekort
import no.nav.aap.arena.KommendeMeldekort
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortStatus

class MeldekortRepositoryPostgresTest {

    @Test
    fun `lagre, oppdatere og hente ut meldekort`() {
        InitTestDatabase.dataSource.transaction {
            val repo = MeldekortRepositoryPostgres(it)
            val ident = nextIdent()
            val kommendeMeldekort = KommendeMeldekort(
                meldekortId = MeldekortId(0),
                type = VANLIG,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
            )
            val historiskMeldekort = HistoriskMeldekort(
                meldekortId = MeldekortId(1),
                type = KORRIGERING,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
                begrunnelseEndring = "Kort",
                mottattIArena = LocalDate.of(2020, 1, 1),
                originalMeldekortId = MeldekortId(0),
                beregningStatus = MeldekortStatus.INNSENDT,
                bruttoBeløp = 0.0
            )
            repo.upsert(ident, listOf(kommendeMeldekort, historiskMeldekort))

            assertEquals(kommendeMeldekort, repo.hent(ident, MeldekortId(0)))
            assertEquals(historiskMeldekort, repo.hent(ident, MeldekortId(1)))

            val nyttMeldekort = HistoriskMeldekort(
                meldekortId = MeldekortId(0),
                type = VANLIG,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
                begrunnelseEndring = null,
                mottattIArena = null,
                originalMeldekortId = null,
                beregningStatus = MeldekortStatus.INNSENDT,
                bruttoBeløp = 0.0
            )

            repo.upsert(ident, nyttMeldekort)
            assertEquals(nyttMeldekort, repo.hent(ident, MeldekortId(0)))
        }
    }

    @Test
    fun `hent batch av meldekort`() {
        InitTestDatabase.dataSource.transaction {
            val repo = MeldekortRepositoryPostgres(it)
            val ident = nextIdent()
            val kommendeMeldekort = KommendeMeldekort(
                meldekortId = MeldekortId(0),
                type = VANLIG,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
            )
            val historiskMeldekort = HistoriskMeldekort(
                meldekortId = MeldekortId(1),
                type = KORRIGERING,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
                begrunnelseEndring = "Kort",
                mottattIArena = LocalDate.of(2020, 1, 1),
                originalMeldekortId = MeldekortId(0),
                beregningStatus = MeldekortStatus.INNSENDT,
                bruttoBeløp = 0.0
            )
            val ikkeHentesUtMeldekort = KommendeMeldekort(
                meldekortId = MeldekortId(2),
                type = VANLIG,
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
                kanKorrigeres = false,
            )
            repo.upsert(ident, listOf(kommendeMeldekort, historiskMeldekort, ikkeHentesUtMeldekort))
            repo.upsert(nextIdent(), kommendeMeldekort.copy(type = KORRIGERING))

            val meldekort = repo.hent(ident, listOf(MeldekortId(0), MeldekortId(1)))

            assertEquals(2, meldekort.size)
            assertEquals(setOf(kommendeMeldekort, historiskMeldekort), meldekort.toSet())

            val historiskeMeldekort = repo.hentAlleHistoriskeMeldekort(ident)
            assertEquals(1, historiskeMeldekort.size)
            assertEquals(setOf(historiskMeldekort), historiskeMeldekort.toSet())
        }
    }
}