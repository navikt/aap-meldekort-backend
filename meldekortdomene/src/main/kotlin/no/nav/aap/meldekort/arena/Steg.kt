package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.StegNavn.*

enum class StegNavn {
    BEKREFT_SVARER_ÆRLIG,
    JOBBET_I_MELDEPERIODEN,
    TIMER_ARBEIDET,
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
            true -> JOBBET_I_MELDEPERIODEN
            false -> KVITTERING
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object JobbetIMeldeperiodenSteg : Steg {
    override val navn: StegNavn
        get() = JOBBET_I_MELDEPERIODEN

    override fun nesteSteg(skjema: Skjema, innloggetBruker: InnloggetBruker): StegNavn {
        return when (skjema.payload.harDuJobbet) {
            null -> error("kan ikke gå videre uten å ha svart")
            else -> TIMER_ARBEIDET
        }
    }
}

class TimerArbeidetSteg(
    private val skjemaService: SkjemaService,
) : Steg {
    override val navn: StegNavn
        get() = TIMER_ARBEIDET


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
