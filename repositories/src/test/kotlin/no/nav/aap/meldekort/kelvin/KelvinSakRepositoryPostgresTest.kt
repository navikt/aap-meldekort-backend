package no.nav.aap.meldekort.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class KelvinSakRepositoryPostgresTest {
    val sak1 = Fagsaknummer("111")
    val sak2 = Fagsaknummer("222")
    val fnr1 = Ident("1".repeat(11))
    val fnr2 = Ident("2".repeat(11))
    val fnr3 = Ident("3".repeat(11))

    val periode1 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2))
    val periode2 = Periode(LocalDate.of(2019, 12, 30), LocalDate.of(2020, 1, 2))
    val periode3 = Periode(LocalDate.of(2020, 10, 1), LocalDate.of(2020, 11, 22))
    val periode4 = Periode(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 22))


    @Test
    fun `endring og lesing av meldeperioder `() {
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = KelvinSakRepositoryPostgres(connection)

            repo.upsertSak(sak1, periode4, listOf(fnr1), listOf())
            repo.upsertSak(sak2, periode4, listOf(fnr3), listOf(periode1, periode2))

            assertEquals(listOf(), repo.hentMeldeperioder(fnr1, sak1))
            assertEquals(listOf(), repo.hentMeldeperioder(fnr2, sak1))
            assertEquals(listOf(periode2, periode1), repo.hentMeldeperioder(fnr3, sak2))
            repo.hentSak(fnr1, periode4.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak1), it?.referanse)
                assertEquals(periode4, it?.rettighetsperiode)
            }
            repo.hentSak(fnr3, periode4.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak2), it?.referanse)
                assertEquals(periode4, it?.rettighetsperiode)
            }

            repo.upsertSak(sak1, periode2, listOf(fnr1, fnr2), listOf(periode1, periode3))

            assertEquals(listOf(periode1, periode3), repo.hentMeldeperioder(fnr1, sak1))
            assertEquals(listOf(periode1, periode3), repo.hentMeldeperioder(fnr2, sak1))
            assertEquals(listOf(periode2, periode1), repo.hentMeldeperioder(fnr3, sak2))

            repo.hentSak(fnr1, periode2.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak1), it?.referanse)
                assertEquals(periode2, it?.rettighetsperiode)
            }
            repo.hentSak(fnr3, periode4.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak2), it?.referanse)
                assertEquals(periode4, it?.rettighetsperiode)
            }

            repo.upsertSak(sak1, periode2, listOf(fnr1, fnr2), listOf(periode2, periode3))
            assertEquals(listOf(periode2, periode3), repo.hentMeldeperioder(fnr1, sak1))
            assertEquals(listOf(periode2, periode3), repo.hentMeldeperioder(fnr2, sak1))
            assertEquals(listOf(periode2, periode1), repo.hentMeldeperioder(fnr3, sak2))
            repo.hentSak(fnr1, periode2.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak1), it?.referanse)
                assertEquals(periode2, it?.rettighetsperiode)
            }
            repo.hentSak(fnr3, periode4.fom).also {
                assertEquals(FagsakReferanse(FagsystemNavn.KELVIN, sak2), it?.referanse)
                assertEquals(periode4, it?.rettighetsperiode)
            }
        }
    }
}