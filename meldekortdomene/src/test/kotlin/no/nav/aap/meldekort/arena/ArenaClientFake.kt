package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import java.time.LocalDate

open class ArenaClientFake(
    val kommendeMeldekort: MutableList<ArenaMeldekort>? = null,
    val historiskeMeldekort: MutableList<ArenaMeldekort>? = null,
) : ArenaClient {

    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe> {
        return listOf(
            ArenaMeldegruppe(
                fodselsnr = innloggetBruker.ident.asString,
                meldegruppeKode = "ATTF",
                datoFra = LocalDate.parse("2024-10-09"),
                datoTil = LocalDate.parse("2025-12-17"),
                hendelsesdato = LocalDate.parse("2024-12-12"),
                statusAktiv = "J",
                begrunnelse = "Iverksatt vedtak",
                styrendeVedtakId = 37059486
            ),
            ArenaMeldegruppe(
                fodselsnr = innloggetBruker.ident.asString,
                meldegruppeKode = "ARBS",
                datoFra = LocalDate.parse("2025-12-18"),
                datoTil = null,
                hendelsesdato = LocalDate.parse("2024-12-12"),
                statusAktiv = "J",
                begrunnelse = "Aktivert med ingen ytelser",
                styrendeVedtakId = null
            )
        )
    }

    override fun person(innloggetBruker: InnloggetBruker): ArenaPerson? {
        return ArenaPerson(
            personId = innloggetBruker.ident.asString.hashCode().toLong(),
            etternavn = "Etternavnson",
            fornavn = "Fornavnson",
            maalformkode = "",
            meldeform = "",
            arenaMeldekortListe = kommendeMeldekort ?: return null,
        )
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson {
        return ArenaPerson(
            personId = innloggetBruker.ident.asString.hashCode().toLong(),
            etternavn = "Etternavnson",
            fornavn = "Fornavnson",
            maalformkode = "",
            meldeform = "",
            arenaMeldekortListe = historiskeMeldekort?.takeLast(antallMeldeperioder)
                ?: throw Exception("bruker finnes ikke"),
        )
    }

    override fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): ArenaMeldekortdetaljer {
        TODO("Not yet implemented")
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): MeldekortId {
        TODO("Not yet implemented")
    }

    override fun sendInn(
        innloggetBruker: InnloggetBruker,
        request: ArenaMeldekortkontrollRequest
    ): MeldekortkontrollResponse {
        TODO("Not yet implemented")
    }
}