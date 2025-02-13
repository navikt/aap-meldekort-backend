package no.nav.aap

import no.nav.aap.sak.Saksnummer
import java.time.Instant

data class UtfyllingId(val asLong: Long)

class Utfylling(
    val id: UtfyllingId,

    val ident: Ident,
    val saksnummer: Saksnummer,

    val opprettet: Instant,
    val sistEndret: Instant,
    val lukket: Instant?,
    val status: Status,
) {
    enum class Status {
        ÅPEN,
        SENDT_INN,
        AVBRUTT,
    }
}
