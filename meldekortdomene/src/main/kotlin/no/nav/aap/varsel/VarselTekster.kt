package no.nav.aap.varsel

data class VarselTekster(
    val nb: String,
    val nn: String,
    val en: String
)

val TEKSTER_BESKJED_FREMTIDIG_OPPLYSNINGSBEHOV = VarselTekster(nb = "", nn = "", en = "")

val TEKSTER_OPPGAVE_OPPLYSNINGSBEHOV = VarselTekster(nb = "", nn = "", en = "")

val TEKSTER_OPPGAVE_MELDEPLIKTPERIODE = VarselTekster(nb = "", nn = "", en = "")