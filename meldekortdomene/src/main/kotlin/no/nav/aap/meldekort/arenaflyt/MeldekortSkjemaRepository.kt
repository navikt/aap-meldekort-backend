package no.nav.aap.meldekort.arenaflyt

import no.nav.aap.meldekort.Ident

interface MeldekortSkjemaRepository {
    fun loadMeldekorttilstand(ident: Ident, meldekortId: Long, flyt: Flyt): Meldekorttilstand?
    fun storeMeldekorttilstand(meldekorttilstand: Meldekorttilstand): Meldekorttilstand
}