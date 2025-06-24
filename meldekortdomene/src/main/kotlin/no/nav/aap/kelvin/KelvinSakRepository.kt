package no.nav.aap.kelvin
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.Sak
import java.time.LocalDate

interface KelvinSakRepository: Repository {
    fun upsertSak(
        saksnummer: Fagsaknummer,
        sakenGjelderFor: Periode,
        identer: List<Ident>,
        meldeperioder: List<Periode>,
        meldeplikt: List<Periode>,
        opplysningsbehov: List<Periode>,
        status: KelvinSakStatus?,
    )
    fun hentMeldeperioder(saksnummer: Fagsaknummer): List<Periode>
    fun hentSak(ident: Ident, påDag: LocalDate): KelvinSak?
    fun hentMeldeplikt(saksnummer: Fagsaknummer): List<Periode>
    fun hentOpplysningsbehov(saksnummer: Fagsaknummer): List<Periode>
}