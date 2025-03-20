package no.nav.aap.kelvin
import no.nav.aap.Periode
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.sak.Fagsaknummer

interface KelvinSakRepository: Repository {
    fun upsertMeldeperioder(saksnummer: Fagsaknummer, meldeperioder: List<Periode>)
}