package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.utfylling.Utfylling
import java.time.Instant

interface PdfgenGateway: Gateway {
    fun genererPdf(ident: Ident, mottatt: Instant, meldekort: Meldekort, utfylling: Utfylling): ByteArray
}