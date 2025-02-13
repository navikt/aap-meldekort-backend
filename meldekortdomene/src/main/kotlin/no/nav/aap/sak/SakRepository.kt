package no.nav.aap.sak

import no.nav.aap.Ident
import no.nav.aap.lookup.repository.Repository

interface SakRepository: Repository {
    fun hentSaker(ident: Ident): Saker
}