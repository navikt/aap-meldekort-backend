package no.nav.aap.meldekort.arena

class InnsendingFeiletException(
    val innsendingFeil: List<ArenaService.InnsendingFeil>
): Exception()