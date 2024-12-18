package no.nav.aap.meldekort.journalføring

interface JoarkRepository {
    fun bestillJournalføring(journalpost: JoarkClient.Journalpost)
    fun hentNeste(): JoarkClient.Journalpost?
    fun markerUtført(key: Any)
}