package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.arena.ArenaClient.KortType.KORRIGERT_ELEKTRONISK
import no.nav.aap.meldekort.arenaflyt.InnsendtMeldekort
import java.time.LocalDate

data class ArenaMeldekortkontrollRequest(
    val meldekortId: Long,
    val fnr: String,
    val personId: Long,
    val kortType: ArenaClient.KortType,
    val meldedato: LocalDate,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val meldegruppe: String,
    val begrunnelse: String?,
    val meldekortdager: List<MeldekortkontrollFravaer>,
    val arbeidet: Boolean,
    val kilde: String,
    val arbeidssoker: Boolean = false,
    val annetFravaer: Boolean = false,
    val kurs: Boolean = false,
    val syk: Boolean = false,
) {
    data class MeldekortkontrollFravaer(
        val dato: LocalDate,
        val arbeidTimer: Double,
        val syk: Boolean = false,
        val kurs: Boolean = false,
        val annetFravaer: Boolean = false,
    )

    companion object {
        fun konstruer(innsendtMeldekort: InnsendtMeldekort, meldekortdetaljer: ArenaMeldekortdetaljer): ArenaMeldekortkontrollRequest {
            val meldekortdager = innsendtMeldekort.timerArbeidet.map { timerArbeidet ->
                MeldekortkontrollFravaer(
                    dato = timerArbeidet.dato,
                    arbeidTimer = timerArbeidet.timer ?: 0.0,
                )
            }

            return ArenaMeldekortkontrollRequest(
                meldekortId = innsendtMeldekort.meldekortId,
                fnr = meldekortdetaljer.fodselsnr,
                personId = meldekortdetaljer.personId,
                kilde = AAP_KODE,
                kortType = meldekortdetaljer.kortType,
                meldedato = if (meldekortdetaljer.kortType == KORRIGERT_ELEKTRONISK && meldekortdetaljer.meldeDato != null) meldekortdetaljer.meldeDato else LocalDate.now(),
                periodeFra = innsendtMeldekort.meldeperiode.fom,
                periodeTil = innsendtMeldekort.meldeperiode.tom,
                meldegruppe = meldekortdetaljer.meldegruppe,
                arbeidet = innsendtMeldekort.harDuJobbet,
                begrunnelse = if (meldekortdetaljer.kortType == KORRIGERT_ELEKTRONISK) TODO() else null,
                meldekortdager = meldekortdager
            )
        }
    }
}

data class MeldekortkontrollResponse(
    val meldekortId: Long = 0,
    val kontrollStatus: String = "",
    val feilListe: List<MeldekortkontrollFeil> = emptyList(),
    val oppfolgingListe: List<MeldekortkontrollFeil> = emptyList()
) {
    data class MeldekortkontrollFeil(
        var kode: String,
        var params: List<String>? = null
    )

    fun validerVellykket() {
        val ok = kontrollStatus in arrayOf("OK", "OKOPP")

        val feil = feilListe.map { feil ->
            ArenaInnsendingFeiletException.InnsendingFeil(feil.kode, feil.params)
        }

        if (!ok || feil.isNotEmpty()) {
            throw ArenaInnsendingFeiletException(feil)
        }
    }
}
