package no.nav.aap.meldekort.journalføring

import no.nav.aap.journalføring.DokarkivGateway

class DokarkivGatewayFake: DokarkivGateway {
    override fun oppdater(
        journalpost: DokarkivGateway.Journalpost,
        forsøkFerdigstill: Boolean
    ): DokarkivGateway.JournalpostResponse {
        return DokarkivGateway.JournalpostResponse(
            journalpostId = 0,
            melding = null,
            journalpostferdigstilt = !forsøkFerdigstill,
            dokumenter = listOf(),
        )
    }
}