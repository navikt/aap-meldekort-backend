package no.nav.aap.meldekort

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.arena.ArenaMeldegruppe
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.ArenaMeldekortdetaljer
import no.nav.aap.arena.ArenaMeldekortkontrollRequest
import no.nav.aap.arena.ArenaPerson
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortkontrollResponse

open class FakeArenaGateway : ArenaGateway {
    var historiskeMeldekort: ArenaPerson? = null
    var korrigertMeldekort: ArenaMeldekort? = null

    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe> {
        return listOf()
    }

    override fun person(innloggetBruker: InnloggetBruker): ArenaPerson? {
        return null
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson {
        return historiskeMeldekort!!
    }

    override fun meldekortdetaljer(
        innloggetBruker: InnloggetBruker, meldekortId: MeldekortId
    ): ArenaMeldekortdetaljer {
        val meldekort = historiskeMeldekort?.arenaMeldekortListe?.find {
            it.meldekortId == meldekortId
        } ?: error("meldekort ikke funnet")


        return ArenaMeldekortdetaljer(
            id = "1",
            personId = 1,
            fodselsnr = innloggetBruker.ident.asString,
            meldekortId = meldekortId,
            meldeperiode = meldekort.meldeperiode,
            meldegruppe = meldekort.hoyesteMeldegruppe,
            arkivnokkel = "",
            kortType = meldekort.kortType,
            meldeDato = null,
            lestDato = null,
            sporsmal = null,
            begrunnelse = null,
        )
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): MeldekortId {
        val meldekort = requireNotNull(korrigertMeldekort)
        historiskeMeldekort = historiskeMeldekort?.copy(
            arenaMeldekortListe = historiskeMeldekort?.arenaMeldekortListe.orEmpty() + meldekort
        )
        return meldekort.meldekortId
    }

    override fun sendInn(
        innloggetBruker: InnloggetBruker, request: ArenaMeldekortkontrollRequest
    ): MeldekortkontrollResponse {
        return MeldekortkontrollResponse(
            meldekortId = request.meldekortId.asLong,
            kontrollStatus = "OK",
            feilListe = emptyList(),
            oppfolgingListe = emptyList(),
        )
    }
}

object FakeArenaGatewaySingleton : FakeArenaGateway()