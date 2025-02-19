package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Fagsaknummer
import java.time.Instant
import java.util.*

data class UtfyllingReferanse(val asUuid: UUID) {
    companion object {
        fun ny(): UtfyllingReferanse {
            return UtfyllingReferanse(UUID.randomUUID())
        }
    }
}

data class Utfylling(
    val referanse: UtfyllingReferanse,
    val fagsystem: FagsystemNavn,
    val fagsaknummer: Fagsaknummer,
    val ident: Ident,
    val periode: Periode,
    val flyt: UtfyllingFlyt,
    val aktivtSteg: UtfyllingSteg,
    val svar: Svar,
    val opprettet: Instant,
    val sistEndret: Instant,
) {
    init {
        check(aktivtSteg in flyt.steg)
    }

    val erAvsluttet: Boolean
        get() = aktivtSteg == flyt.steg.last()
}
