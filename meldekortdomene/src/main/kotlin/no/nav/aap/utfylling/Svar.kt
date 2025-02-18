package no.nav.aap.utfylling

import no.nav.aap.Periode
import java.time.LocalDate

data class Svar(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidet>,
    val stemmerOpplysningene: Boolean?,
) {
    companion object {
        fun tomt(periode: Periode): Svar {
            return Svar(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = periode.map { TimerArbeidet(it, null) },
                stemmerOpplysningene = null
            )
        }
    }
}

data class TimerArbeidet(
    val dato: LocalDate,
    val timer: Double?,
)

