package no.nav.aap.meldekort.kontrakt.sak

import no.nav.aap.meldekort.kontrakt.Periode

public class MeldeperioderV0(
    public val saksnummer: String,
    public val sakenGjelderFor: Periode,
    public val meldeperioder: List<Periode>,
)