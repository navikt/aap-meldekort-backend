package no.nav.aap.meldekort.arena

import java.time.LocalDate
import java.util.*

data class ArenaMeldekortdetaljer(
    val id: String,
    val personId: Long,
    val fodselsnr: String,
    val meldekortId: MeldekortId,
    val meldeperiode: String,
    val meldegruppe: String,
    val arkivnokkel: String,
    val kortType: ArenaClient.KortType,
    val meldeDato: LocalDate? = null,
    val lestDato: LocalDate? = null,
    val sporsmal: Sporsmal? = null,
    val begrunnelse: String? = ""
) {
    data class Sporsmal(
        val arbeidssoker: Boolean? = null,
        val arbeidet: Boolean? = null,
        val syk: Boolean? = null,
        val annetFravaer: Boolean? = null,
        val kurs: Boolean? = null,
        val forskudd: Boolean? = null,
        val signatur: Boolean? = null,
        val meldekortDager: List<MeldekortDag>? = null
    )

    data class MeldekortDag(
        val dag: Int = 0,
        val arbeidetTimerSum: Double? = null,
        val syk: Boolean? = null,
        val annetFravaer: Boolean? = null,
        val kurs: Boolean? = null,
        val meldegruppe: String? = null
    )

    fun timerArbeidet(fom: LocalDate): List<TimerArbeidet>? {
        val aktivitetsdager = MutableList(14) { index ->
            TimerArbeidet(null, fom.plusDays(index.toLong()))
        }

        if (this.sporsmal?.meldekortDager == null) return null

        this.sporsmal.meldekortDager.forEach { dag ->
            if (dag.arbeidetTimerSum != null && dag.arbeidetTimerSum > 0) {
                aktivitetsdager[dag.dag - 1] = aktivitetsdager[dag.dag - 1].copy(timer = dag.arbeidetTimerSum)
            }
        }

        return aktivitetsdager
    }

    data class Aktivitet(
        val uuid: UUID,
        val type: AktivitetsType,
        val timer: Double?
    ) {
        enum class AktivitetsType {
            Arbeid,
            Syk,
            Utdanning,
            Fravaer
        }
    }
}