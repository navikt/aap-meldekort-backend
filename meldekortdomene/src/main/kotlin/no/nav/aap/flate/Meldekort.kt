package no.nav.aap.flate

import no.nav.aap.Ident
import no.nav.aap.opplysningsplikt.TimerArbeidet
import java.time.Instant

class Meldekort(
    val ident: Ident,
    val levert: Instant,
    val opplysninger: List<TimerArbeidet>,
)
