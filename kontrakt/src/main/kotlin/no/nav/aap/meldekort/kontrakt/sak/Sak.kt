package no.nav.aap.meldekort.kontrakt.sak

import no.nav.aap.meldekort.kontrakt.Periode
import java.time.LocalDate

public class MeldeperioderV0(
    public val identer: List<String>,
    public val saksnummer: String,
    public val sakenGjelderFor: Periode,
    public val meldeperioder: List<Periode>,
    public val sakStatus: SakStatus? = null,

    /* Når medlemmet skal melde seg. */
    public val meldeplikt: List<Periode> = emptyList(),

    /* Hvilke perioder vi åpner for at medlemmet
     * gir oss opplysninger gjennom meldekortet.
     */
    public val opplysningsbehov: List<Periode> = emptyList(),
)

public enum class SakStatus {
    UTREDES,
    LØPENDE,
    AVSLUTTET,
}

public data class BehandslingsflytUtfyllingRequest(
    val saksnummer: String,
    val ident: String,
    val periode: Periode,
    val harDuJobbet: Boolean,
    val dager: List<TimerArbeidetDto>,
    val sakenGjelderFor: Periode
)

public data class TimerArbeidetDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)