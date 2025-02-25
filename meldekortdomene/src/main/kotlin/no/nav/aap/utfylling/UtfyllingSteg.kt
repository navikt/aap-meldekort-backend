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

interface UtfyllingSteg {
    val navn: UtfyllingStegNavn

    val erTeknisk: Boolean
        get() = navn.erTeknisk

    fun erRelevant(utfylling: Utfylling): Boolean {
        return true
    }

    /* Sjekk om formkrav oppfylles. Sjekkes også for ikke-relevante steg. */
    fun oppfyllerFormkrav(utfylling: Utfylling): Boolean {
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

        if (timerArbeidet.any { it.dato !in utfylling.periode }) {
            return false
        }

        val riktigFormat = timerArbeidet.all {
            val timer = it.timer ?: return@all true
            timer in 0.0..24.0 && (timer.toString().let { it.endsWith(".0") || it.endsWith(".5") })
        }
        if (!riktigFormat) {
            return false
        }

        val harRegistrertTimer = timerArbeidet.any { it.timer != null && it.timer > 0.0 }

        return harRegistrertTimer || utfylling.svar.harDuJobbet == false
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
    private val arenaSakService: ArenaSakService,
): UtfyllingSteg {
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
): UtfyllingSteg {
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
