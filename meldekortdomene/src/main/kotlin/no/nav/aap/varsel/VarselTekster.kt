package no.nav.aap.varsel

data class VarselTekster(
    val nb: String,
    val nn: String,
    val en: String
)


val TEKSTER_OPPGAVE_MELDEPLIKTPERIODE = VarselTekster(
    nb = "Du må sende meldekort. Klikk her for å fylle det ut, og sende det inn.",
    nn = "Du må sende meldekort. Klikk her for å fylle det ut, og sende det inn.",
    en = "You need to send us your employment status form. Click here to fill out and submit."
)