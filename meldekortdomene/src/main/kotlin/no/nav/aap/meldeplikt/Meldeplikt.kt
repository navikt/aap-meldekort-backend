package no.nav.aap.meldeplikt

import no.nav.aap.opplysningsplikt.TimerArbeidet
import java.time.LocalDate

class Medlem(
    val sistMeldtSeg: LocalDate?,
    val opplysninger: List<TimerArbeidet>,
)
