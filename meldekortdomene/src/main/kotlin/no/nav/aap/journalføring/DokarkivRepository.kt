package no.nav.aap.journalføring

interface DokarkivRepository {
    fun bestillJournalføring(journalpost: DokarkivGateway.Journalpost)
    fun hentNeste(): DokarkivGateway.Journalpost?
    fun markerUtført(key: Any)
}