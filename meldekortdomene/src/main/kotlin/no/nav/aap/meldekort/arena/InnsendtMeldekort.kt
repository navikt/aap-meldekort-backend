package no.nav.aap.meldekort.arena

@Suppress("unused")
class InnsendtMeldekort(
    val meldekortId: Long,
    val svarerDuSant: Boolean,
    val harDuJobbet: Boolean,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean
)