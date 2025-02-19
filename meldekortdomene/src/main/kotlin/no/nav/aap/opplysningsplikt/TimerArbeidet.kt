package no.nav.aap.opplysningsplikt

import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.Instant
import java.time.LocalDate

data class TimerArbeidet(
    val registreringstidspunkt: Instant,
    val utfylling: UtfyllingReferanse,
    val fagsak: FagsakReferanse,
    val dato: LocalDate,
    val timerArbeidet: Double?,
)
