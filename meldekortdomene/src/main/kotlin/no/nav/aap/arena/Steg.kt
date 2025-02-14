package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.StegNavn.*

enum class StegNavn {
    BEKREFT_SVARER_ÆRLIG,
    SPØRSMÅL,
    UTFYLLING,
    STEMMER_OPPLYSNINGENE,
    INNSENDING_VANLIG_MELDEKKORT,
    KVITTERING,
}


object BekreftSvarerÆrligSteg : Steg {
    override val navn: StegNavn
        get() = BEKREFT_SVARER_ÆRLIG

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        return when (skjema.payload.svarerDuSant) {
            true -> Stegutfall.Fortsett(skjema)
            else -> Stegutfall.Avklaringspunkt(skjema)
        }
    }
}

object JobbetIMeldeperiodenSteg : Steg {
    override val navn: StegNavn
        get() = SPØRSMÅL

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        val besvart = skjema.payload.run {
            harDuJobbet != null /* TODO: resten av spørsmålene her */
        }
        return if (besvart)
            Stegutfall.Fortsett(skjema)
        else
            Stegutfall.Avklaringspunkt(skjema)
    }
}

object TimerArbeidetSteg : Steg {
    override val navn: StegNavn
        get() = UTFYLLING

    override fun skalKjøres(skjema: Skjema): Boolean {
        return skjema.payload.run {
            harDuJobbet == true /* TODO: reste av spørsmålene her */
        }
    }

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        /* sjekk om nødvendlige feltene er fyllt inn */
        return Stegutfall.Fortsett(skjema)
    }
}

class StemmerOpplysningeneSteg(private val skjemaService: SkjemaService) : Steg {
    override val navn: StegNavn
        get() = STEMMER_OPPLYSNINGENE

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        return when (skjema.payload.stemmerOpplysningene) {
            true -> Stegutfall.Fortsett(skjema)
            else -> Stegutfall.Avklaringspunkt(skjema)
        }
    }
}

class InnsendingVanligMeldekort(private val skjemaService: SkjemaService): Steg {
    override val navn: StegNavn
        get() = INNSENDING_VANLIG_MELDEKKORT

    override fun erTekniskSteg(): Boolean {
        return true
    }

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        skjemaService.sendInn(skjema, innloggetBruker)
        return Stegutfall.Fortsett(skjema)
    }
}

object KvitteringSteg : Steg {
    override val navn: StegNavn
        get() = KVITTERING

    override fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall {
        return Stegutfall.Avklaringspunkt(skjema)
    }
}
