package no.nav.aap.meldekort

import no.nav.aap.meldekort.arena.Arena

object FakeArena : Arena {
    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<Arena.Meldegruppe> {
        return listOf()
    }

    override fun person(innloggetBruker: InnloggetBruker): Arena.Person? {
        return null
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): Arena.Person {
        TODO("Not yet implemented")
    }

    override fun meldekortdetaljer(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long
    ): Arena.Meldekortdetaljer {
        TODO("Not yet implemented")
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long {
        return 0
    }

    override fun sendInn(
        innloggetBruker: InnloggetBruker,
        request: Arena.MeldekortkontrollRequest
    ): Arena.MeldekortkontrollResponse {
        TODO("Not yet implemented")
    }
}