package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker

object UtfyllingFlytOrkestrator {
    class FlytResultat(
        val utfylling: Utfylling,
        val feil: Exception? = null,
    )

    fun kjør(innloggetBruker: InnloggetBruker, utfylling: Utfylling): FlytResultat {
        check(!utfylling.erAvsluttet)

        val formkravFeilerSteg = oppfyllerFormkrav(utfylling)
        if (formkravFeilerSteg != null) {
            return FlytResultat(utfylling.copy(aktivtSteg = formkravFeilerSteg))
        }

        val nesteAktiveSteg = nesteIkketekniskeSteg(utfylling)

        try {
            utførEffekter(innloggetBruker, utfylling, nesteAktiveSteg)
            return FlytResultat(utfylling.copy(aktivtSteg = nesteAktiveSteg))
        } catch (exceptioin: Exception) {
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
            if (forbiAktivt && !steg.erTeknisk) {
                return steg
            }
            if (steg == utfylling.aktivtSteg) {
                forbiAktivt = true
            }
        }

        return utfylling.flyt.steg.last().also {
            check(!it.erTeknisk)
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
                /* TODO: ønsker nok mark savepoint her. */
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(Unit)
    }
}

