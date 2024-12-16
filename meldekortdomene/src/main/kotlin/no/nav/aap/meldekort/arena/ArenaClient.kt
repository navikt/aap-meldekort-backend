package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker

interface ArenaClient {
    fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe>

    fun person(innloggetBruker: InnloggetBruker): ArenaPerson?

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson

    fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: Long): ArenaMeldekortdetaljer

    fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long

    fun sendInn(innloggetBruker: InnloggetBruker, request: ArenaMeldekortkontrollRequest): MeldekortkontrollResponse

    enum class KortType(val code: String) {
        ORDINAER("01"),
        ERSTATNING("03"),
        RETUR("04"),
        ELEKTRONISK("05"),
        AAP("06"),
        ORDINAER_MANUELL("07"),
        MASKINELT_OPPDATERT("08"),
        MANUELL_ARENA("09"),
        KORRIGERT_ELEKTRONISK("10");

        companion object {
            fun getByCode(code: String): KortType {
                return requireNotNull(entries.find { it.code == code }) {
                    "ukjent KortType $code fra arena"
                }
            }
        }
    }
}