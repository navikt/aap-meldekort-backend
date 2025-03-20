package no.nav.aap.kelvin

import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.Fagsaknummer

class KelvinSakRepositoryImpl(private val connection: DBConnection): KelvinSakRepository {
    override fun upsertMeldeperioder(saksnummer: Fagsaknummer, meldeperioder: List<Periode>) {
    }
}