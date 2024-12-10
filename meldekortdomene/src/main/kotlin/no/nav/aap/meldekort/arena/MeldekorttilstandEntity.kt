package no.nav.aap.meldekort.arena

data class MeldekorttilstandEntity(
    val meldekortId: Long,
    val meldekortskjema: Meldekortskjema,
    val steg: StegNavn,
    val aktiv: Boolean,
) {
    companion object {
        fun fraDomene(meldekorttilstand: Meldekorttilstand, aktiv: Boolean): MeldekorttilstandEntity {
            return MeldekorttilstandEntity(
                meldekortId = meldekorttilstand.meldekortId,
                meldekortskjema = meldekorttilstand.meldekortskjema,
                steg = meldekorttilstand.steg.navn,
                aktiv = aktiv
            )
        }
    }

    fun tilDomene(flyt: Flyt): Meldekorttilstand {
        return Meldekorttilstand(
            meldekortId = meldekortId,
            meldekortskjema = meldekortskjema,
            steg = flyt.stegForNavn(steg),
        )
    }
}