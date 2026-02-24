package no.nav.aap.opplysningsplikt

import no.nav.aap.InnloggetBruker
import no.nav.aap.utfylling.Fravær
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.PERSISTER_OPPLYSNINGER

class PersisterOpplysningerSteg(
    private val aktivitetsInformasjonRepository: AktivitetsInformasjonRepository,
) : UtfyllingSteg {
    override val navn = PERSISTER_OPPLYSNINGER

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        aktivitetsInformasjonRepository.lagrAktivitetsInformasjon(
            ident = utfylling.ident,
            opplysninger = utfylling.svar.aktivitetsInformasjon.map {
                AktivitetsInformasjon(
                    registreringstidspunkt = utfylling.sistEndret,
                    utfylling = utfylling.referanse,
                    fagsak = utfylling.fagsak,
                    dato = it.dato,
                    timerArbeidet = it.timer,
                    fravær = it.fravær?.name?.let { Fravær.valueOf(it) }
                )
            }
        )
        /* TODO: lagre dato meldt seg separat, kan pr. i dag utledes av hvilke datoer man har gitt opplysninger om.  */
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

