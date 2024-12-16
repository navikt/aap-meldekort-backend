package no.nav.aap.meldekort.arena

import java.time.LocalDate

class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

class Meldeperiode(
    val meldekortId: Long,
    val periode: Periode,
    val kanSendesFra: LocalDate,
    val kanSendes: Boolean,
    val kanEndres: Boolean,
    val type: Type,
) {
    enum class Type {
        ORDINÆRT,
        ETTERREGISTRERT,
    }
}