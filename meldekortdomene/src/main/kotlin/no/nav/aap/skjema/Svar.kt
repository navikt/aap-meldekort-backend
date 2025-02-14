package no.nav.aap.skjema

import no.nav.aap.Periode
import java.time.LocalDate

data class Svar(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidet>,
    val stemmerOpplysningene: Boolean?,
) {
    companion object {
        fun tomtSkjema(periode: Periode): Svar {
            return Svar(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = periode.map { TimerArbeidet(null, it) },
                stemmerOpplysningene = null
            )
        }
    }
}

data class TimerArbeidet(
    val timer: Double?,
    val dato: LocalDate,
)

