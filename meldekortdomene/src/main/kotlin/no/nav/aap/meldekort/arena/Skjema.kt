package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.arena.SkjemaTilstand.UTKAST

data class Skjema(
    val flyt: SkjemaFlyt,
    val tilstand: SkjemaTilstand,
    val meldekortId: Long,
    val ident: Ident,
    val steg: Steg,
    val meldeperiode: Periode,
    val payload: InnsendingPayload,
) {

    fun medSteg(steg: StegNavn): Skjema {
        return copy(steg = flyt.stegForNavn(steg))
    }

    fun nesteSteg(innloggetBruker: InnloggetBruker): Skjema {
        require(tilstand == UTKAST)
        return copy(
            steg = flyt.nesteSteg(this, innloggetBruker),
        )
    }

    fun validerUtkast() {
        check(tilstand == UTKAST)
        // TODO: kast exception hvis validering feil?
    }
}

enum class SkjemaTilstand {
    UTKAST,
    FORSØKER_Å_SENDE_TIL_ARENA,
    SENDT_ARENA,
    JOURNALFØRT,
}