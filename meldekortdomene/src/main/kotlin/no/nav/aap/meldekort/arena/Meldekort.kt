package no.nav.aap.meldekort.arena

data class Meldekort(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
) {
    companion object {
        fun tomtMeldekort(): Meldekort {
            return Meldekort(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = List(14) { null },
                stemmerOpplysningene = null
            )
        }
    }
}
