package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import java.util.concurrent.atomic.AtomicInteger


private var nextIdent = AtomicInteger()

fun nextIdent(): Ident {
    return Ident(nextIdent.incrementAndGet().toString().padStart(11, '0'))
}
