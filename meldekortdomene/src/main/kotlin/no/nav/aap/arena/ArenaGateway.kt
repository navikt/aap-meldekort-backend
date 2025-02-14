package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.MeldekortType.ETTERREGISTRERING
import no.nav.aap.arena.MeldekortType.KORRIGERING
import no.nav.aap.arena.MeldekortType.UKJENT
import no.nav.aap.arena.MeldekortType.VANLIG
import no.nav.aap.lookup.gateway.Gateway

interface ArenaGateway: Gateway {
    fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe>

    fun person(innloggetBruker: InnloggetBruker): ArenaPerson?

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson

    fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): ArenaMeldekortdetaljer

    fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): MeldekortId

    fun sendInn(innloggetBruker: InnloggetBruker, request: ArenaMeldekortkontrollRequest): MeldekortkontrollResponse

    enum class KortType(val code: String, val meldekortType: MeldekortType) {
        /* Meldekortet er opprettet på initiativ av bruker for å kunne sende inn en korrigering. */
        KORRIGERT_ELEKTRONISK("10", KORRIGERING),

        /* Dette er vanlige meldekort som er opprettet av Arena. */
        ELEKTRONISK("05", VANLIG),

        /* Dette er et meldekort opprettet av Arena som er bakover i tid. */
        MANUELL_ARENA("09", ETTERREGISTRERING),

        /* Historiske korttyper: */
        ORDINAER("01", UKJENT), ERSTATNING("03", UKJENT), RETUR("04", UKJENT), AAP("06", UKJENT),
        MASKINELT_OPPDATERT("08", UKJENT), ORDINAER_MANUELL("07", UKJENT);

        companion object {
            fun getByCode(code: String): KortType {
                return requireNotNull(entries.find { it.code == code }) {
                    "ukjent KortType $code fra arena"
                }
            }
        }
    }
}