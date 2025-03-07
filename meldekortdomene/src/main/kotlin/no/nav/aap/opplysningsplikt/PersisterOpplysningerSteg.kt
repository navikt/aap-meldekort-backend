package no.nav.aap.opplysningsplikt

import no.nav.aap.InnloggetBruker
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.PERSISTER_OPPLYSNINGER

class PersisterOpplysningerSteg(
    private val timerArbeidetRepository: TimerArbeidetRepository,
): UtfyllingSteg {
    override val navn = PERSISTER_OPPLYSNINGER

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        timerArbeidetRepository.lagrTimerArbeidet(
            ident = utfylling.ident,
            opplysninger = utfylling.svar.timerArbeidet.map {
                TimerArbeidet(
                    registreringstidspunkt = utfylling.sistEndret,
                    utfylling = utfylling.referanse,
                    fagsak = utfylling.fagsak,
                    dato = it.dato,
                    timerArbeidet = it.timer,
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

