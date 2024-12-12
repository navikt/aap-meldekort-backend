package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.arena.StegNavn.*
import org.slf4j.LoggerFactory

enum class StegNavn {
    BEKREFT_SVARER_ÆRLIG,
    JOBBET_I_MELDEPERIODEN,
    TIMER_ARBEIDET,
    KVITTERING,
}

interface Steg {
    val navn: StegNavn
    fun nesteSteg(meldekorttilstand: Meldekorttilstand): StegNavn
}

object BekreftSvarerÆrlig: Steg {
    override val navn: StegNavn
        get() = BEKREFT_SVARER_ÆRLIG

    override fun nesteSteg(meldekorttilstand: Meldekorttilstand): StegNavn {
        return when (meldekorttilstand.meldekortskjema.svarerDuSant) {
            true -> JOBBET_I_MELDEPERIODEN
            false -> KVITTERING
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object JobbetIMeldeperioden: Steg {
    override val navn: StegNavn
        get() = JOBBET_I_MELDEPERIODEN

    override fun nesteSteg(meldekorttilstand: Meldekorttilstand): StegNavn {
        return when (meldekorttilstand.meldekortskjema.harDuJobbet) {
            null -> error("kan ikke gå videre uten å ha svart")
            else -> TIMER_ARBEIDET
        }
    }
}

class TimerArbeidet(
    private val meldekortService: MeldekortService,
): Steg {
    private val log = LoggerFactory.getLogger(this::class.java)
    override val navn: StegNavn
        get() = TIMER_ARBEIDET

    override fun nesteSteg(meldekorttilstand: Meldekorttilstand): StegNavn {
        return when (meldekorttilstand.meldekortskjema.stemmerOpplysningene) {
            true -> {
                try {
                    meldekortService.sendInn(meldekorttilstand.meldekortskjema, meldekorttilstand.meldekortId)
                    KVITTERING
                } catch (e: Exception) {
                    log.error(
                        "innsending av meldekort med meldekortId: ${meldekorttilstand.meldekortId} til arena feilet", e
                    )
                    throw InnsendingFeiletException()
                }
            }
            false -> KVITTERING
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object Kvittering: Steg {
    override val navn: StegNavn
        get() = KVITTERING

    override fun nesteSteg(meldekorttilstand: Meldekorttilstand): StegNavn {
        error("Kvittering er alltid siste steg")
    }
}
