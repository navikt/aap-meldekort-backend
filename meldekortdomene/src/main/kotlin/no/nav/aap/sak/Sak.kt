package no.nav.aap.sak

import no.nav.aap.Periode.Klassifikasjon.DAGEN_ER_ETTER_PERIODEN
import no.nav.aap.Periode.Klassifikasjon.DAGEN_ER_FØR_PERIODEN
import no.nav.aap.Periode.Klassifikasjon.DAGER_ER_I_PERIODEN
import no.nav.aap.meldeplikt.Medlem
import no.nav.aap.meldeplikt.Meldeperiode
import java.time.LocalDate
import no.nav.aap.Periode

data class Saksnummer(val asString: String)

enum class Fagsystem {
    ARENA, KELVIN,
}

class Sak(
    val fagsystem: Fagsystem,
    val saksnummer: Saksnummer?,
    val rettighetsperiode: Periode,
    val meldeperioder: List<Meldeperiode>,
    val fritakFraMeldeplikt: List<Periode>,
) {
    fun burdeMeldeSeg(medlemmet: Medlem, idag: LocalDate): Boolean {
        val nesteMeldevindu = førstkommendeMeldevindu(medlemmet, idag) ?: return false
        return idag in nesteMeldevindu
    }

    fun førstkommendeMeldevindu(medlemmet: Medlem, idag: LocalDate): Periode? {
        /* Å ha fritak er som å ha meldt seg. */
        val sistMeldtSeg = maxOf(
            medlemmet.sistMeldtSeg ?: LocalDate.MIN,
            senesteFritak(maks = idag) ?: LocalDate.MIN,
        )
        return meldeperioder
            .asSequence()
            .filterNot { meldeperiode -> meldeperiode.erAlleredeMeldt(sistMeldtSeg) }
            .filter { meldeperiode -> meldeperiode.erForSentÅMeldeSeg(idag) }
            .minByOrNull { it.meldevindu }
            ?.meldevindu
    }

    private fun senesteFritak(maks: LocalDate): LocalDate? {
        return fritakFraMeldeplikt
            .mapNotNull { fritaksperiode ->
                when (fritaksperiode.klassifiser(maks)) {
                    DAGEN_ER_FØR_PERIODEN -> null
                    DAGER_ER_I_PERIODEN -> maks
                    DAGEN_ER_ETTER_PERIODEN -> fritaksperiode.tom
                }
            }
            .maxOrNull()
    }
}

