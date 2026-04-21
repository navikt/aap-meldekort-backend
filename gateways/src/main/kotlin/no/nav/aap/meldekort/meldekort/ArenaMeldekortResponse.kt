package no.nav.aap.meldekort.meldekort

import no.nav.aap.arena.ArenaMeldekort
import java.time.LocalDate

internal data class ArenaMeldekortResponse(
    val personId: Long,
    val etternavn: String,
    val fornavn: String,
    val maalformkode: String,
    val meldeform: String,
    val antallGjenstaaendeFeriedager: Int? = 0,
    val meldekortListe: List<ArenaMeldekort>? = null,
    val fravaerListe: List<ArenaFravaerType>? = null,
)

internal data class ArenaFravaerType(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val type: String,
)
