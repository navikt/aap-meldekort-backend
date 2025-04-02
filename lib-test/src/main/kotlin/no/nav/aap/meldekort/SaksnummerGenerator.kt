package no.nav.aap.meldekort

import no.nav.aap.sak.Fagsaknummer

val saksnummerGenerator = generateSequence(1111L) { it + 1 }
    .map { Fagsaknummer(it.toString()) }
    .iterator()