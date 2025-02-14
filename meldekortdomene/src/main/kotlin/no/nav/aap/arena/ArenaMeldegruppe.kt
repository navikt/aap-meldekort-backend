package no.nav.aap.arena

import java.time.LocalDate

const val AAP_KODE = "ATTF" // attføringsstønad

data class ArenaMeldegruppe(
    val fodselsnr: String,
    val meldegruppeKode: String,
    val datoFra: LocalDate,
    val datoTil: LocalDate? = null,
    val hendelsesdato: LocalDate,
    val statusAktiv: String,
    val begrunnelse: String,
    val styrendeVedtakId: Long? = null
) {
    fun erMeldegruppeAAP(): Boolean {
        return meldegruppeKode == AAP_KODE
    }
}