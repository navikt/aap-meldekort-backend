package no.nav.aap.meldekort.arenaflyt

import no.nav.aap.meldekort.InnloggetBruker

class Meldekorttilstand(
    val meldekortId: Long,
    val meldekortskjema: Meldekortskjema,
    val steg: Steg,
    val meldeperiode: Periode,
) {
    fun nesteTilstand(flyt: Flyt, innloggetBruker: InnloggetBruker): Meldekorttilstand {
        return Meldekorttilstand(
            meldekortId = meldekortId,
            steg = flyt.nesteSteg(this, innloggetBruker),
            meldekortskjema = meldekortskjema,
            meldeperiode = meldeperiode
        )
    }

    fun innsendtMeldekort(): InnsendtMeldekort {
        return InnsendtMeldekort(
            meldekortId = meldekortId,
            svarerDuSant = requireNotNull(meldekortskjema.svarerDuSant) {
                "alle felter må være fylt ut for innsending, svarerDuSant er ikke fylt ut"
            },
            harDuJobbet = requireNotNull(meldekortskjema.harDuJobbet) {
                "alle felter må være fylt ut for innsending, harDuJobbet er ikke fylt ut"
            },
            timerArbeidet = meldekortskjema.timerArbeidet,
            stemmerOpplysningene = requireNotNull(meldekortskjema.stemmerOpplysningene) {
                "alle felter må være fylt ut for innsending, stemmerOpplysningene er ikke fylt ut"
            },
            meldeperiode = meldeperiode
        )
    }
}