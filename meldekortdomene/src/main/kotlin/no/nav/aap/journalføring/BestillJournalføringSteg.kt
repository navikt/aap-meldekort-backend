package no.nav.aap.journalføring

import no.nav.aap.InnloggetBruker
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.BESTILL_JOURNALFØRING

class BestillJournalføringSteg: UtfyllingSteg {
    override val navn = BESTILL_JOURNALFØRING
    override val erTeknisk = true

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        /* TODO */
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

