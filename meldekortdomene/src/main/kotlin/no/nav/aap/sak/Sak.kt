package no.nav.aap.sak

import no.nav.aap.Periode

data class Fagsaknummer(val asString: String)


class Sak(
    val fagsystemNavn: FagsystemNavn,
    val fagsaknummer: Fagsaknummer,
    val rettighetsperiode: Periode,
//    val meldeperioder: List<Meldeperiode>,
//    val fritakFraMeldeplikt: List<Periode>,
) {
//    fun burdeMeldeSeg(medlemmet: Medlem, idag: LocalDate): Boolean {
//        val nesteMeldevindu = førstkommendeMeldevindu(medlemmet, idag) ?: return false
//        return idag in nesteMeldevindu
//    }

//    fun førstkommendeMeldevindu(medlemmet: Medlem, idag: LocalDate): Periode? {
//        /* Å ha fritak er som å ha meldt seg. */
//        val sistMeldtSeg = maxOf(
//            medlemmet.sistMeldtSeg ?: LocalDate.MIN,
//            senesteFritak(maks = idag) ?: LocalDate.MIN,
//        )
//        return meldeperioder
//            .asSequence()
//            .filterNot { meldeperiode -> meldeperiode.erAlleredeMeldt(sistMeldtSeg) }
//            .filter { meldeperiode -> meldeperiode.erForSentÅMeldeSeg(idag) }
//            .minByOrNull { it.meldevindu }
//            ?.meldevindu
//    }

//    private fun senesteFritak(maks: LocalDate): LocalDate? {
//        return fritakFraMeldeplikt
//            .mapNotNull { fritaksperiode ->
//                when (fritaksperiode.klassifiser(maks)) {
//                    DAGEN_ER_FØR_PERIODEN -> null
//                    DAGER_ER_I_PERIODEN -> maks
//                    DAGEN_ER_ETTER_PERIODEN -> fritaksperiode.tom
//                }
//            }
//            .maxOrNull()
//    }
}

