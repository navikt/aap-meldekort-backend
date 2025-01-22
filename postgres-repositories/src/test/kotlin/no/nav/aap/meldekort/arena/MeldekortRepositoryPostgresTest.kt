package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.arena.MeldekortType.KORRIGERING
import no.nav.aap.meldekort.arena.MeldekortType.VANLIG
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class MeldekortRepositoryPostgresTest {

    @Test
    fun `lagre, oppdatere og hente ut meldekort`() {
        val repo = MeldekortRepositoryPostgres(InitTestDatabase.dataSource)
        val ident = nextIdent()
        val kommendeMeldekort = KommendeMeldekort(
            meldekortId = 0,
            type = VANLIG,
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
            kanKorrigeres = false,
        )
        val historiskMeldekort = HistoriskMeldekort(
            meldekortId = 1,
            type = KORRIGERING,
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
            kanKorrigeres = false,
            begrunnelseEndring = "Kort",
            mottattIArena = LocalDate.of(2020, 1, 1),
            originalMeldekortId = 0,
            beregningStatus = MeldekortStatus.INNSENDT,
            bruttoBeløp = 0.0
        )
        repo.upsert(ident, listOf(kommendeMeldekort, historiskMeldekort))

        assertEquals(kommendeMeldekort, repo.hent(ident, 0))
        assertEquals(historiskMeldekort, repo.hent(ident, 1))

        val nyttMeldekort = HistoriskMeldekort(
            meldekortId = 0,
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
        assertEquals(nyttMeldekort, repo.hent(ident, 0))
    }

    @Test
    fun `hent batch av meldekort`() {
        val repo = MeldekortRepositoryPostgres(InitTestDatabase.dataSource)
        val ident = nextIdent()
        val kommendeMeldekort = KommendeMeldekort(
            meldekortId = 0,
            type = VANLIG,
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
            kanKorrigeres = false,
        )
        val historiskMeldekort = HistoriskMeldekort(
            meldekortId = 1,
            type = KORRIGERING,
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
            kanKorrigeres = false,
            begrunnelseEndring = "Kort",
            mottattIArena = LocalDate.of(2020, 1, 1),
            originalMeldekortId = 0,
            beregningStatus = MeldekortStatus.INNSENDT,
            bruttoBeløp = 0.0
        )
        val ikkeHentesUtMeldekort = KommendeMeldekort(
            meldekortId = 2,
            type = VANLIG,
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)),
            kanKorrigeres = false,
        )
        repo.upsert(ident, listOf(kommendeMeldekort, historiskMeldekort, ikkeHentesUtMeldekort))
        repo.upsert(nextIdent(), kommendeMeldekort.copy(type = KORRIGERING))

        val meldekort = repo.hent(ident, listOf(0, 1))

        assertEquals(2, meldekort.size)
        assertEquals(setOf(kommendeMeldekort, historiskMeldekort), meldekort.toSet())

        val historiskeMeldekort = repo.hentAlleHistoriskeMeldekort(ident)
        assertEquals(1, historiskeMeldekort.size)
        assertEquals(setOf(historiskMeldekort), historiskeMeldekort.toSet())
    }
}