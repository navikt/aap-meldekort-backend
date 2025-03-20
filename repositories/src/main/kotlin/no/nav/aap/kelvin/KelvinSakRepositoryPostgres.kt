package no.nav.aap.kelvin

import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.sak.Fagsaknummer

class KelvinSakRepositoryPostgres(private val connection: DBConnection): KelvinSakRepository {
    override fun upsertMeldeperioder(saksnummer: Fagsaknummer, meldeperioder: List<Periode>) {
    }

    companion object: Factory<KelvinSakRepository> {
        override fun konstruer(connection: DBConnection): KelvinSakRepository {
            return KelvinSakRepositoryPostgres(connection)
        }
    }
}