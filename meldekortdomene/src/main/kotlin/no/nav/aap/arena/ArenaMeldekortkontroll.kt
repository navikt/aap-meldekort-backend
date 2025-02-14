package no.nav.aap.arena

import no.nav.aap.arena.ArenaGateway.KortType.KORRIGERT_ELEKTRONISK
import no.nav.aap.skjema.Skjema
import java.time.LocalDate

data class ArenaMeldekortkontrollRequest(
    val meldekortId: MeldekortId,
    val fnr: String,
    val personId: Long,
    val kortType: ArenaGateway.KortType,
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
        fun konstruer(
            skjema: Skjema,
            meldekortdetaljer: ArenaMeldekortdetaljer
        ): ArenaMeldekortkontrollRequest {
            val meldekortdager = skjema.svar.timerArbeidet.map { timerArbeidet ->
                MeldekortkontrollFravaer(
                    dato = timerArbeidet.dato,
                    arbeidTimer = if (skjema.svar.harDuJobbet == true) timerArbeidet.timer ?: 0.0 else 0.0,
                )
            }

            return ArenaMeldekortkontrollRequest(
                meldekortId = skjema.meldekortId,
                fnr = meldekortdetaljer.fodselsnr,
                personId = meldekortdetaljer.personId,
                kilde = AAP_KODE,
                kortType = meldekortdetaljer.kortType,
                meldedato = if (meldekortdetaljer.kortType == KORRIGERT_ELEKTRONISK && meldekortdetaljer.meldeDato != null) meldekortdetaljer.meldeDato else LocalDate.now(),
                periodeFra = skjema.meldeperiode.fom,
                periodeTil = skjema.meldeperiode.tom,
                meldegruppe = meldekortdetaljer.meldegruppe,
                arbeidet = requireNotNull(skjema.svar.harDuJobbet) {
                    "alle felter må være fylt ut for innsending, harDuJobbet er ikke fylt ut"
                },
                begrunnelse = if (meldekortdetaljer.kortType == KORRIGERT_ELEKTRONISK)
                    "Korrigering av arbeidstimer"
                else
                    null,
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
            throw ArenaInnsendingFeiletException(kontrollStatus, feil)
        }
    }
}
