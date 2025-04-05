package no.nav.aap.kelvin

import no.nav.aap.Periode
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak

class KelvinSak(
    val saksnummer: Fagsaknummer,
    val status: KelvinSakStatus?,
    override val rettighetsperiode: Periode,
): Sak {
    override val referanse = FagsakReferanse(FagsystemNavn.KELVIN, saksnummer)
}

enum class KelvinSakStatus {
    UTREDES,
    LØPENDE,
    AVSLUTTET,
}