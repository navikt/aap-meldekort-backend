package no.nav.aap.meldekort.kontrakt.sak

import no.nav.aap.meldekort.kontrakt.Periode

public class MeldeperioderV0(
    public val saksnummer: Saksnummer,
    public val status: Status,
    public val periode: Periode,
    public val meldeperioder: List<Periode>,
)