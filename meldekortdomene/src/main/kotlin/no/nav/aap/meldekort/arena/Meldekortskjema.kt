package no.nav.aap.meldekort.arena

data class Meldekortskjema(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Double?>,
    val stemmerOpplysningene: Boolean?,
) {
    companion object {
        fun tomtMeldekortskjema(): Meldekortskjema {
            return Meldekortskjema(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = List(14) { null },
                stemmerOpplysningene = null
            )
        }
    }
}
