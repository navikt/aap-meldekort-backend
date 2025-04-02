package no.nav.aap.meldekort

import no.nav.aap.Ident

val fødselsnummerGenerator = generateSequence(11223312345L) { it + 1 }
    .map { Ident(it.toString()) }
    .iterator()
