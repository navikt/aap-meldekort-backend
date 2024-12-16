package no.nav.aap.meldekort.arenaflyt

import java.time.LocalDate

class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
): Iterable<LocalDate> {
    override fun iterator(): Iterator<LocalDate> {
        return generateSequence(fom) { it.plusDays(1) }.takeWhile { it <= tom }.iterator()
    }
}

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