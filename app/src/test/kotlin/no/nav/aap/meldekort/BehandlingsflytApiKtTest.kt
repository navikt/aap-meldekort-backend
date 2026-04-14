package no.nav.aap.meldekort

import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.Ident
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.Fagsaknummer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BehandlingsflytApiKtTest {
        private val app = AppInstance()

    @Test
    fun `meldeplikt settes korrekt i databasen ved melding fra behandlingsflyt`() {
        val rettighetsperiode = Periode(fom = LocalDate.of(2025, 3, 3), tom = LocalDate.of(2025, 4, 27))

        val meldeperioder = listOf(
            "2025-03-03" to "2025-03-16",
            "2025-03-17" to "2025-03-30",
            "2025-03-31" to "2025-04-13",
            "2025-04-14" to "2025-04-27"
        ).map{ (fom, tom) -> Periode(LocalDate.parse(fom), LocalDate.parse(tom))}

        val meldeplikt = listOf(
            "2025-03-31" to "2025-04-07",
            "2025-04-14" to "2025-04-21",
            "2025-04-28" to "2025-05-05"
        ).map { (fom, tom) -> Periode(LocalDate.parse(fom), LocalDate.parse(tom))}

        val fnr = fødselsnummerGenerator.next()
        val meldedata = MeldeperioderV0(
            sakenGjelderFor = rettighetsperiode,
            saksnummer = "SAKSNUMMER",
            personIdenter = listOf(Ident(fnr.asString, fnr.aktiv)),
            meldeperioder = meldeperioder,
            meldeplikt = meldeplikt
        )

        app.behandlingsflytPost<Unit, MeldeperioderV0>(meldedata)
        app.dataSource.transaction { dbConnection ->
            val kelvinSakRepository = postgresRepositoryRegistry.provider(dbConnection).provide<KelvinSakRepository>()
            val meldepliktFraRepository = kelvinSakRepository.hentMeldeplikt(Fagsaknummer("SAKSNUMMER"))
            assertEquals(meldepliktFraRepository.map { Periode(it.fom, it.tom) }, meldeplikt)

            val meldeperioderFraRepository = kelvinSakRepository.hentMeldeperioder(Fagsaknummer("SAKSNUMMER"))
            assertEquals(meldeperioderFraRepository.map { Periode(it.fom, it.tom)}, meldeperioder)
        }
    }
 }