package no.nav.aap.sak

import no.nav.aap.Periode

enum class FagsystemNavn {
    ARENA, KELVIN,
}

data class Fagsaknummer(val asString: String)

data class FagsakReferanse(
    val system: FagsystemNavn,
    val nummer: Fagsaknummer,
)

interface Sak {
    val referanse: FagsakReferanse
    val rettighetsperiode: Periode
}
