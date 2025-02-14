package no.nav.aap.journalføring

/* TODO: flytt til testmappe når integrasjon i dev er klar. */
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