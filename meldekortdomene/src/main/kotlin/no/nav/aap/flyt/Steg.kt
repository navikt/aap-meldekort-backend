package no.nav.aap.flyt

import no.nav.aap.InnloggetBruker
import no.nav.aap.flyt.StegNavn.INTRODUKSJON
import no.nav.aap.flyt.StegNavn.KVITTERING
import no.nav.aap.flyt.StegNavn.SPØRSMÅL
import no.nav.aap.flyt.StegNavn.STEMMER_OPPLYSNINGENE
import no.nav.aap.flyt.StegNavn.UTFYLLING
import no.nav.aap.skjema.Skjema
import no.nav.aap.skjema.SkjemaService

enum class StegNavn {
    INTRODUKSJON,
    SPØRSMÅL,
    UTFYLLING,
    STEMMER_OPPLYSNINGENE,
    INNSENDING_VANLIG_MELDEKKORT,
    KVITTERING,
}


object BekreftSvarerÆrligSteg : Steg {
    override val navn: StegNavn
        get() = INTRODUKSJON

    override fun oppfyllerFormkrav(skjema: Skjema): Boolean {
        return skjema.svar.svarerDuSant == true
    }
}

object JobbetIMeldeperiodenSteg : Steg {
    override val navn: StegNavn
        get() = SPØRSMÅL

    override fun oppfyllerFormkrav(skjema: Skjema): Boolean {
        return skjema.svar.harDuJobbet != null
    }
}

object TimerArbeidetSteg : Steg {
    override val navn: StegNavn
        get() = UTFYLLING

    override fun erRelevant(skjema: Skjema): Boolean {
        return skjema.svar.run {
            harDuJobbet == true /* TODO: reste av spørsmålene her */
        }
    }

    override fun oppfyllerFormkrav(skjema: Skjema): Boolean {
        val timerArbeidet = skjema.svar.timerArbeidet
        return timerArbeidet.any { it.timer != null && it.timer > 0 } && timerArbeidet.all {
            val timer = it.timer ?: return@all true
            timer >= 0 && timer <= 24 && (timer.toString().let { it.endsWith(".0") || it.endsWith(".5") })
        }
    }
}

class StemmerOpplysningeneSteg(private val skjemaService: SkjemaService) : Steg {
    override val navn: StegNavn
        get() = STEMMER_OPPLYSNINGENE

    override fun oppfyllerFormkrav(skjema: Skjema): Boolean {
        return skjema.svar.stemmerOpplysningene == true
    }

    override fun nesteEffekt(innloggetBruker: InnloggetBruker, skjema: Skjema) {
        skjemaService.sendInn(skjema, innloggetBruker)
    }
}

object KvitteringSteg : Steg {
    override val navn: StegNavn
        get() = KVITTERING

    override fun oppfyllerFormkrav(skjema: Skjema): Boolean {
        return true
    }
}
