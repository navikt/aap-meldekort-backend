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
    fun nesteSteg(meldekortskjema: Meldekortskjema): NesteUtfall
}

sealed interface NesteUtfall
object InnsendingFeilet: NesteUtfall
class GåTilSteg(val steg: StegNavn): NesteUtfall

object BekreftSvarerÆrlig: Steg {
    override val navn: StegNavn
        get() = BEKREFT_SVARER_ÆRLIG

    override fun nesteSteg(meldekortskjema: Meldekortskjema): NesteUtfall {
        return when (meldekortskjema.svarerDuSant) {
            true -> GåTilSteg(JOBBET_I_MELDEPERIODEN)
            false -> GåTilSteg(KVITTERING)
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object JobbetIMeldeperioden: Steg {
    override val navn: StegNavn
        get() = JOBBET_I_MELDEPERIODEN

    override fun nesteSteg(meldekortskjema: Meldekortskjema): NesteUtfall {
        return when (meldekortskjema.harDuJobbet) {
            null -> error("kan ikke gå videre uten å ha svart")
            else -> GåTilSteg(TIMER_ARBEIDET)
        }
    }
}

class TimerArbeidet(
    private val meldekortService: MeldekortService,
): Steg {
    private val log = LoggerFactory.getLogger(this::class.java)
    override val navn: StegNavn
        get() = TIMER_ARBEIDET

    override fun nesteSteg(meldekortskjema: Meldekortskjema): NesteUtfall {
        return when (meldekortskjema.stemmerOpplysningene) {
            true -> {
                try {
                    meldekortService.sendInn(meldekortskjema)
                    GåTilSteg(KVITTERING)
                } catch (e: Exception) {
                    log.error("innsending av meldekort til arena feilet", e)
                    InnsendingFeilet
                }
            }
            false -> GåTilSteg(KVITTERING)
            null -> error("kan ikke gå videre uten å ha svart")
        }
    }
}

object Kvittering: Steg {
    override val navn: StegNavn
        get() = KVITTERING

    override fun nesteSteg(meldekortskjema: Meldekortskjema): NesteUtfall {
        error("kan ikke gå videre uten å ha svart")
    }
}
