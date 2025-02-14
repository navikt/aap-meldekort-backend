package no.nav.aap.arena

data class ArenaPerson(
    val personId: Long,
    val etternavn: String,
    val fornavn: String,
    val maalformkode: String,
    val meldeform: String,
    val arenaMeldekortListe: List<ArenaMeldekort>,
)