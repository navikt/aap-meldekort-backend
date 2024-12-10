package no.nav.aap.meldekort.arena

data class Meldekortskjema(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
) {
    companion object {
        fun tomtMeldekort(): Meldekortskjema {
            return Meldekortskjema(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = List(14) { null },
                stemmerOpplysningene = null
            )
        }
    }

    fun innsendtMeldekort(meldekortId: Long): InnsendtMeldekort {
        return InnsendtMeldekort(
            meldekortId = meldekortId,
            svarerDuSant = requireNotNull(svarerDuSant) {
                "alle felter må være fylt ut for innsending, svarerDuSant er ikke fylt ut"
            },
            harDuJobbet = requireNotNull(harDuJobbet) {
                "alle felter må være fylt ut for innsending, harDuJobbet er ikke fylt ut"
            },
            timerArbeidet = timerArbeidet,
            stemmerOpplysningene = requireNotNull(stemmerOpplysningene) {
                "alle felter må være fylt ut for innsending, stemmerOpplysningene er ikke fylt ut"
            },
        )
    }
}
