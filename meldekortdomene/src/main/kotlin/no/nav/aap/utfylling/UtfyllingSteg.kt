package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaSakService
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_KORRIGERING
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_VANLIG
import no.nav.aap.utfylling.UtfyllingStegNavn.BEKREFT
import no.nav.aap.utfylling.UtfyllingStegNavn.INTRODUKSJON
import no.nav.aap.utfylling.UtfyllingStegNavn.KVITTERING
import no.nav.aap.utfylling.UtfyllingStegNavn.SPØRSMÅL
import no.nav.aap.utfylling.UtfyllingStegNavn.UTFYLLING
import java.time.LocalDate

enum class UtfyllingStegNavn(val erTeknisk: Boolean = false) {
    INTRODUKSJON,
    SPØRSMÅL,
    UTFYLLING,
    BEKREFT,
    ARENAKONTROLL_VANLIG(erTeknisk = true),
    ARENAKONTROLL_KORRIGERING(erTeknisk = true),
    PERSISTER_OPPLYSNINGER(erTeknisk = true),
    BESTILL_JOURNALFØRING(erTeknisk = true),
    KVITTERING,
}

typealias Formkrav = Map<String, (Utfylling) -> Boolean>

interface UtfyllingSteg {
    val navn: UtfyllingStegNavn

    val erTeknisk: Boolean
        get() = navn.erTeknisk

    fun erRelevant(utfylling: Utfylling): Boolean {
        return true
    }

    /* Sjekk om formkrav oppfylles. Sjekkes også for ikke-relevante steg. */
    val formkrav: Formkrav get() = mapOf()

    /* Må være idempotent! */
    fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
    }
}


object IntroduksjonSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = INTRODUKSJON

    override val formkrav: Formkrav = mapOf(
        "VIL_GI_RIKTIGE_SVAR" to { utfylling ->
            utfylling.svar.svarerDuSant == true
        }
    )
}

object SpørsmålSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = SPØRSMÅL

    override var formkrav: Formkrav = mapOf(
        "MÅ_SVARE_OM_JOBBET" to { utfylling ->
            utfylling.svar.harDuJobbet != null
        }
    )
}

object TimerArbeidetSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = UTFYLLING

    override fun erRelevant(utfylling: Utfylling): Boolean {
        return utfylling.svar.run {
            harDuJobbet == true
        }
    }

    override val formkrav: Formkrav = mapOf(
        "OPPGIR_OPPLYSNINGER_INNENFOR_PERIODE" to { utfylling ->
            utfylling.svar.timerArbeidet.all { it.dato in utfylling.periode }
        },
        "HELE_ELLER_HALVE_TIMER" to { utfylling ->
            utfylling.svar.timerArbeidet.all {
                val timer = it.timer ?: return@all true
                timer in 0.0..24.0 && (timer.toString().let { it.endsWith(".0") || it.endsWith(".5") })
            }
        },
        "HAR_REGISTRERT_TIMER_OM_ARBEIDET" to { utfylling ->
            if (utfylling.svar.harDuJobbet == true) {
                utfylling.svar.timerArbeidet.any { it.timer != null && it.timer > 0.0 }
            } else {
                true
            }
        },
        "HAR_IKKE_REGISTRERT_TIMER_OM_IKKE_ARBEIDET" to { utfylling ->
            if (utfylling.svar.harDuJobbet == false) {
                utfylling.svar.timerArbeidet.all { it.timer == null || it.timer == 0.0 }
            } else {
                true
            }
        }
    )
}

object StemmerOpplysningeneSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = BEKREFT

    override val formkrav: Formkrav = mapOf(
        "MÅ_BEKREFTE" to { utfylling ->
            utfylling.svar.stemmerOpplysningene == true
        },
        "KUN_HISTORISKE_OPPLYSNINGER" to { utfylling ->
            utfylling.periode.tom < LocalDate.now()
        }
    )
}

class ArenaKontrollVanligSteg(
    private val arenaSakService: ArenaSakService,
) : UtfyllingSteg {
    override val navn = ARENAKONTROLL_VANLIG

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        arenaSakService.sendInnVanlig(innloggetBruker, utfylling)
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

class ArenaKontrollKorrigeringSteg(
    private val arenaSakService: ArenaSakService,
) : UtfyllingSteg {
    override val navn = ARENAKONTROLL_KORRIGERING

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        arenaSakService.sendInnKorrigering(innloggetBruker, utfylling)
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

object KvitteringSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = KVITTERING
}
