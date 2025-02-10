package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.StegNavn.*

enum class StegNavn {
    BEKREFT_SVARER_ÆRLIG,
    SPØRSMÅL,
    UTFYLLING,
    STEMMER_OPPLYSNINGENE,
    KVITTERING,
}

interface Steg {
    val navn: StegNavn
    fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn
}

object BekreftSvarerÆrligSteg : Steg {
    override val navn: StegNavn
        get() = BEKREFT_SVARER_ÆRLIG

    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        return when (skjema.payload.svarerDuSant) {
            true -> SPØRSMÅL
            false -> KVITTERING
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object JobbetIMeldeperiodenSteg : Steg {
    override val navn: StegNavn
        get() = SPØRSMÅL

    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        return when (skjema.payload.harDuJobbet) {
         /* hvis minst en er sann */   true -> UTFYLLING
        /* hvis alle er usanne */    false -> STEMMER_OPPLYSNINGENE
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object TimerArbeidetSteg : Steg {
    override val navn: StegNavn
        get() = UTFYLLING


    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        return STEMMER_OPPLYSNINGENE
    }
}

class StemmerOpplysningeneSteg(private val skjemaService: SkjemaService) : Steg {
    override val navn: StegNavn
        get() = STEMMER_OPPLYSNINGENE

    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        return when (skjema.payload.stemmerOpplysningene) {
            true -> {
                skjemaService.sendInn(skjema, innloggetBruker)
                KVITTERING
            }

            false -> KVITTERING
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object KvitteringSteg : Steg {
    override val navn: StegNavn
        get() = KVITTERING

    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        error("Kvittering er alltid siste steg")
    }
}
