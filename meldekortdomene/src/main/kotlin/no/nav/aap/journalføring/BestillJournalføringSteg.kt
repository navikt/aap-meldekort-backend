package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.BESTILL_JOURNALFØRING

class BestillJournalføringSteg(
    private val journalføringService: JournalføringService,
) : UtfyllingSteg {
    override val navn = BESTILL_JOURNALFØRING

    override fun utførEffekt(ident: Ident, utfylling: Utfylling) {
        journalføringService.bestillJournalføring(ident, utfylling)
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

