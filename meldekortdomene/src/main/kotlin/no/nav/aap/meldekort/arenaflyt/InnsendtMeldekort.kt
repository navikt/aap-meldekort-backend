package no.nav.aap.meldekort.arenaflyt

@Suppress("unused")
class InnsendtMeldekort(
    val meldekortId: Long,
    val svarerDuSant: Boolean,
    val harDuJobbet: Boolean,
    val timerArbeidet: List<TimerArbeidet>,
    val stemmerOpplysningene: Boolean,
    val meldeperiode: Periode,
)

