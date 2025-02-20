package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import org.slf4j.LoggerFactory

object UtfyllingFlytOrkestrator {
    private val log = LoggerFactory.getLogger(this.javaClass)
    class FlytResultat(
        val utfylling: Utfylling,
        val feil: Exception? = null,
    )

    fun kjør(innloggetBruker: InnloggetBruker, utfylling: Utfylling): FlytResultat {
        check(!utfylling.erAvsluttet)

        val formkravFeilerSteg = oppfyllerFormkrav(utfylling)
        if (formkravFeilerSteg != null) {
            log.info("utfylling oppfyller ikke formkrav for steget ${formkravFeilerSteg.navn}, så setter det som aktivt steg")
            return FlytResultat(utfylling.copy(aktivtSteg = formkravFeilerSteg))
        }

        val nesteAktiveSteg = nesteIkketekniskeSteg(utfylling)
        log.info("neste ikke-tekniske relevante steg er ${nesteAktiveSteg.navn}")

        try {
            utførEffekter(innloggetBruker, utfylling, nesteAktiveSteg)
            log.info("alle effekter opp til ${nesteAktiveSteg.navn} kjørte vellyket, aktivt steg blir ${nesteAktiveSteg.navn}")
            return FlytResultat(utfylling.copy(aktivtSteg = nesteAktiveSteg))
        } catch (exceptioin: Exception) {
            log.info("feil ved kjøring av effekter opp til ${nesteAktiveSteg.navn}, aktivt steg uendret ${utfylling.aktivtSteg.navn}")
            return FlytResultat(utfylling, exceptioin)
        }
    }

    /** Sjekker formkrav fra starten og til og med aktivt steg.
     *
     * Hvis alle formkravene er oppfylt returneres `null`.
     * Om det formkrav ikke er oppfylt returneres første steg
     * hvor formkravet ikke er oppfylt.
     */
    private fun oppfyllerFormkrav(utfylling: Utfylling): UtfyllingSteg? {
        for (steg in utfylling.flyt.steg) {
            if (!steg.oppfyllerFormkrav(utfylling)) {
                return steg
            }

            /* ikke sjekk lenger frem enn det vi er. */
            if (steg == utfylling.aktivtSteg) {
                break
            }
        }

        return null
    }

    private fun nesteIkketekniskeSteg(utfylling: Utfylling): UtfyllingSteg {
        var forbiAktivt = false

        for (steg in utfylling.flyt.steg) {
            if (forbiAktivt && !steg.erTeknisk && steg.erRelevant(utfylling)) {
                return steg
            }
            if (steg == utfylling.aktivtSteg) {
                forbiAktivt = true
            }
        }

        return utfylling.flyt.steg.last().also {
            check(!it.erTeknisk)
            check(it.erRelevant(utfylling))
        }
    }

    private fun utførEffekter(
        innloggetBruker: InnloggetBruker,
        utfylling: Utfylling,
        nesteAktiveSteg: UtfyllingSteg
    ): Result<Unit> {
        for (steg in utfylling.flyt.steg) {
            if (steg == nesteAktiveSteg) {
                break
            }

            try {
                steg.utførEffekt(innloggetBruker, utfylling)
                /* TODO: ønsker nok mark savepoint her. eller? */
            } catch (e: Exception) {
                return Result.failure(Exception("effekt for steg ${steg.navn} feilet", e))
            }
        }
        return Result.success(Unit)
    }
}

