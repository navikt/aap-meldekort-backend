package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Periode
import java.time.LocalDate

interface Meldekort {
    val meldekortId: Long
    val periode: Periode
    val type: MeldekortType
    val kanKorrigeres: Boolean
}

enum class MeldekortType {
    VANLIG,
    ETTERREGISTRERING,
    KORRIGERING,
    UKJENT,
}

/* NB: Oppgitt i prioritert rekkefølge! (????) */
enum class MeldekortStatus {
    /** Bruker har opprettet en korrigering av original rapporteringsperiode */
    KORRIGERT,

    /** Rapporteringsperioden er sendt til beregning. */
    INNSENDT,

    /** Rapporteringsperioden er ferdig behandlet. */
    FERDIG,

    /** Rapporteringsperioden er ferdig behandlet. */
    FEILET,
}

data class KommendeMeldekort(
    override val meldekortId: Long,
    override val type: MeldekortType,
    override val periode: Periode,
    override val kanKorrigeres: Boolean,
) : Meldekort {
    private val tidligsteInnsendingsdato: LocalDate = periode.tom.minusDays(1)
    val kanSendes: Boolean
        get() = tidligsteInnsendingsdato <= LocalDate.now()
}

data class HistoriskMeldekort(
    override val meldekortId: Long,
    override val type: MeldekortType,
    override val periode: Periode,
    override val kanKorrigeres: Boolean,
    val begrunnelseEndring: String?,
    val mottattIArena: LocalDate?,
    val orginalMeldekortId: Long?,
    val beregningStatus: MeldekortStatus,
) : Meldekort {
    fun erLengreIProsessen(other: HistoriskMeldekort): Boolean {
        return beregningStatus.ordinal > other.beregningStatus.ordinal
    }
}
