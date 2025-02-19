package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaService
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_KORRIGERING
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_VANLIG
import no.nav.aap.utfylling.UtfyllingStegNavn.INTRODUKSJON
import no.nav.aap.utfylling.UtfyllingStegNavn.BESTILL_JOURNALFØRING
import no.nav.aap.utfylling.UtfyllingStegNavn.KVITTERING
import no.nav.aap.utfylling.UtfyllingStegNavn.SPØRSMÅL
import no.nav.aap.utfylling.UtfyllingStegNavn.BEKREFT
import no.nav.aap.utfylling.UtfyllingStegNavn.PERSISTER_OPPLYSNINGER
import no.nav.aap.utfylling.UtfyllingStegNavn.UTFYLLING

enum class UtfyllingStegNavn {
    INTRODUKSJON,
    SPØRSMÅL,
    UTFYLLING,
    BEKREFT,
    ARENAKONTROLL_VANLIG,
    ARENAKONTROLL_KORRIGERING,
    PERSISTER_OPPLYSNINGER,
    BESTILL_JOURNALFØRING,
    KVITTERING,
}

interface UtfyllingSteg {
    val navn: UtfyllingStegNavn

    val erTeknisk: Boolean get() = false

    fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
        return true
    }

    fun erRelevant(utfylling: Utfylling): Boolean {
        return true
    }

    /* Må være idempotent! */
    fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
    }
}


object IntroduksjonSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = INTRODUKSJON

    override fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
        return utfylling.svar.svarerDuSant == true
    }
}

object SpørsmålSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = SPØRSMÅL

    override fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
        return utfylling.svar.harDuJobbet != null
    }
}

object TimerArbeidetSteg : UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = UTFYLLING

    override fun erRelevant(utfylling: Utfylling): Boolean {
        return utfylling.svar.run {
            harDuJobbet == true
        }
    }

    override fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
        val timerArbeidet = utfylling.svar.timerArbeidet
        if (timerArbeidet.all { it.timer == null || it.timer == 0.0 }) {
            return false
        }
        return timerArbeidet.all {
            val timer = it.timer ?: return@all true
            timer in 0.0..24.0 && (timer.toString().let { it.endsWith(".0") || it.endsWith(".5") })
        }
    }
}

object StemmerOpplysningeneSteg: UtfyllingSteg {
    override val navn: UtfyllingStegNavn
        get() = BEKREFT

    override fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
        return utfylling.svar.stemmerOpplysningene == true
    }
}

class ArenaKontrollVanligSteg(
    private val arenaService: ArenaService,
): UtfyllingSteg {
    override val navn = ARENAKONTROLL_VANLIG
    override val erTeknisk = true

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        arenaService.sendInnVanlig(innloggetBruker, utfylling)
    }

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingSteg && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

class ArenaKontrollKorrigeringSteg(
    private val arenaService: ArenaService,
): UtfyllingSteg {
    override val navn = ARENAKONTROLL_KORRIGERING
    override val erTeknisk = true

    override fun utførEffekt(innloggetBruker: InnloggetBruker, utfylling: Utfylling) {
        arenaService.sendInnKorrigering(innloggetBruker, utfylling)
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
