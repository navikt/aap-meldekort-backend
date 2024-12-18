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
    val kanSendesFra: LocalDate,
) : Meldekort {
    val kanSendes: Boolean
        get() = kanSendesFra <= LocalDate.now()
}

data class HistoriskMeldekort(
    override val meldekortId: Long,
    override val type: MeldekortType,
    override val periode: Periode,
    override val kanKorrigeres: Boolean,
    val begrunnelseEndring: String?,
    val mottatt: LocalDate?,
    val orginalMeldekortId: Long?,
    val status: MeldekortStatus,
) : Meldekort
