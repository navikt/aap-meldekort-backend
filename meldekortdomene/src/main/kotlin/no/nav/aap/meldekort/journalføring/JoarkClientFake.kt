package no.nav.aap.meldekort.journalføring

/* TODO: flytt til testmappe når integrasjon i dev er klar. */
class JoarkClientFake: JoarkClient {
    override fun oppdater(
        journalpost: JoarkClient.Journalpost,
        forsøkFerdigstill: Boolean
    ): JoarkClient.JournalpostResponse {
        return JoarkClient.JournalpostResponse(
            journalpostId = 0,
            melding = null,
            journalpostferdigstilt = !forsøkFerdigstill,
            dokumenter = listOf(),
        )
    }
}