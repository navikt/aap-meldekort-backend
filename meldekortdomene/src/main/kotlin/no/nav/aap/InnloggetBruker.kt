package no.nav.aap

data class Ident(
    val asString: String,
    val aktiv: Boolean? = true,
)

class InnloggetBruker(
    val ident: Ident,
    val token: String,
)