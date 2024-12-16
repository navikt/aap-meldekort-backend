package no.nav.aap.meldekort.arenaflyt

import java.time.LocalDate

data class Meldekortskjema(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidet>,
    val stemmerOpplysningene: Boolean?,
) {
    companion object {
        fun tomtMeldekortskjema(meldeperiode: Periode): Meldekortskjema {
            return Meldekortskjema(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = meldeperiode.map { TimerArbeidet(null, it) },
                stemmerOpplysningene = null
            )
        }
    }
}

data class TimerArbeidet(
    val timer: Double?,
    val dato: LocalDate,
)
