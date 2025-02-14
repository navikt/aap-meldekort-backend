package no.nav.aap.arena

import no.nav.aap.Periode
import java.time.LocalDate

interface Meldekort {
    val meldekortId: MeldekortId
    val periode: Periode
    val type: MeldekortType
    val kanKorrigeres: Boolean

    val tidligsteInnsendingsdato: LocalDate
        get() = periode.tom.minusDays(1)
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
    OVERSTYRT_AV_ANNET_MELDEKORT,

    /** Rapporteringsperioden er sendt til beregning. */
    INNSENDT,

    /** Rapporteringsperioden er ferdig behandlet. */
    FERDIG,

    /** Rapporteringsperioden er ferdig behandlet. */
    FEILET,
}

data class KommendeMeldekort(
    override val meldekortId: MeldekortId,
    override val type: MeldekortType,
    override val periode: Periode,
    override val kanKorrigeres: Boolean,
) : Meldekort {
    val kanSendes: Boolean
        get() = tidligsteInnsendingsdato <= LocalDate.now()
}

data class HistoriskMeldekort(
    override val meldekortId: MeldekortId,
    override val type: MeldekortType,
    override val periode: Periode,
    override val kanKorrigeres: Boolean,
    val begrunnelseEndring: String?,
    val mottattIArena: LocalDate?,
    val originalMeldekortId: MeldekortId?,
    val beregningStatus: MeldekortStatus,
    val bruttoBeløp: Double?
) : Meldekort {
    fun erLengreIProsessen(other: HistoriskMeldekort): Boolean {
        return beregningStatus.ordinal > other.beregningStatus.ordinal
    }
}

