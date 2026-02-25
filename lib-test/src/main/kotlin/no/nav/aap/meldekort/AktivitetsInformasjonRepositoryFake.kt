package no.nav.aap.meldekort

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.opplysningsplikt.AktivitetsInformasjon
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepository
import no.nav.aap.sak.FagsakReferanse
import java.time.LocalDate

class AktivitetsInformasjonRepositoryFake: AktivitetsInformasjonRepository {
    override fun hentAktivitetsInformasjon(ident: Ident, sak: FagsakReferanse, periode: Periode): List<AktivitetsInformasjon> {
        return listOf()
    }

    override fun lagrAktivitetsInformasjon(ident: Ident, opplysninger: List<AktivitetsInformasjon>) {
    }

    override fun hentSenesteOpplysningsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate? {
        return null
    }
}