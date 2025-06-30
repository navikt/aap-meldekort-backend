package no.nav.aap.varsel

data class VarselTekster(
    val nb: String,
    val nn: String,
    val en: String
)


val TEKSTER_OPPGAVE_MELDEPLIKTPERIODE = VarselTekster(
    nb = "Du må sende meldekort. Klikk her for å fylle det ut, og sende det inn.",
    nn = "", // TODO legg inn tekster
    en = "" // TODO legg inn tekster
)