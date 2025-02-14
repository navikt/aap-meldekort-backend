package no.nav.aap

import java.time.LocalDate
import no.nav.aap.sak.Fagsystem
import no.nav.aap.sak.SakerGateway
import no.nav.aap.sak.Saker

class AnsvarligFlate(
    private val sakerGateway: SakerGateway,
) {
    fun routingForBruker(innloggetBruker: InnloggetBruker): Fagsystem {
        val saker = sakerGateway.hentSaker(innloggetBruker)
        /* TODO: mer logikk hvis det ikke er en åpen sak på dagens dato, men det:
         * 1. nylig har vært en sak,
         * 2. kommer en sak (er dette et mulig scenario?),
         * 3. både 1 og 2.
         */
        val sak = saker.finnSakForDagen(LocalDate.now()) ?: return Fagsystem.ARENA
        return sak.fagsystem
    }

    fun debugSaker(innloggetBruker: InnloggetBruker): Saker {
        return sakerGateway.hentSaker(innloggetBruker)
    }
}