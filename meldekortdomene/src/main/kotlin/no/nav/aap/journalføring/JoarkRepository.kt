package no.nav.aap.journalføring

interface JoarkRepository {
    fun bestillJournalføring(journalpost: JoarkClient.Journalpost)
    fun hentNeste(): JoarkClient.Journalpost?
    fun markerUtført(key: Any)
}