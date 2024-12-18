package no.nav.aap.meldekort

data class Ident(val asString: String)

class InnloggetBruker(
    val ident: Ident,
    val token: String,
)