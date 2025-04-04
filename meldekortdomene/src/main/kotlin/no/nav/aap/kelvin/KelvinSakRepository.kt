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
    )
    fun hentMeldeperioder(ident: Ident, saksnummer: Fagsaknummer): List<Periode>
    fun hentSak(ident: Ident, påDag: LocalDate): Sak?
    fun hentMeldeplikt(ident: Ident, saksnummer: Fagsaknummer): List<Periode>
    fun hentOpplysningsbehov(ident: Ident, saksnummer: Fagsaknummer): List<Periode>
}