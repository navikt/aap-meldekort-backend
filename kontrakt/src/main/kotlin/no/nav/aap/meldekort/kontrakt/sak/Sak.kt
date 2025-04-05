package no.nav.aap.meldekort.kontrakt.sak

import no.nav.aap.meldekort.kontrakt.Periode

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