package no.nav.aap.meldekort

import no.nav.aap.meldekort.arena.ArenaClient
import no.nav.aap.meldekort.arena.ArenaMeldegruppe
import no.nav.aap.meldekort.arena.ArenaMeldekortdetaljer
import no.nav.aap.meldekort.arena.ArenaMeldekortkontrollRequest
import no.nav.aap.meldekort.arena.ArenaPerson
import no.nav.aap.meldekort.arena.MeldekortkontrollResponse

object FakeArenaClient : ArenaClient {
    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe> {
        return listOf()
    }

    override fun person(innloggetBruker: InnloggetBruker): ArenaPerson? {
        return null
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson {
        TODO("Not yet implemented")
    }

    override fun meldekortdetaljer(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long
    ): ArenaMeldekortdetaljer {
        TODO("Not yet implemented")
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long {
        return 0
    }

    override fun sendInn(
        innloggetBruker: InnloggetBruker,
        request: ArenaMeldekortkontrollRequest
    ): MeldekortkontrollResponse {
        TODO("Not yet implemented")
    }
}