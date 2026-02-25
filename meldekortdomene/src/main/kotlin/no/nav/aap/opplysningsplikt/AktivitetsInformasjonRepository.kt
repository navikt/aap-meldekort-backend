package no.nav.aap.opplysningsplikt

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.sak.FagsakReferanse
import java.time.LocalDate


interface AktivitetsInformasjonRepository: Repository {
    /** @return registrerte opplysninger, sorted på dato (ascending) */
    fun hentAktivitetsInformasjon(
        ident: Ident,
        sak: FagsakReferanse,
        periode: Periode,
    ): List<AktivitetsInformasjon>

    fun lagrAktivitetsInformasjon(
        ident: Ident,
        opplysninger: List<AktivitetsInformasjon>,
    )

    fun hentSenesteOpplysningsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate?
}