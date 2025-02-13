package no.nav.aap

data class Ident(val asString: String)

class InnloggetBruker(
    val ident: Ident,
    val token: String,
)