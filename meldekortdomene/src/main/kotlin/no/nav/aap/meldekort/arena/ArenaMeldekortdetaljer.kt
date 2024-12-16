package no.nav.aap.meldekort.arena

import java.time.LocalDate
import java.util.*

data class ArenaMeldekortdetaljer(
    val id: String,
    val personId: Long,
    val fodselsnr: String,
    val meldekortId: Long,
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
        val arbeidetTimerSum: Float? = null,
        val syk: Boolean? = null,
        val annetFravaer: Boolean? = null,
        val kurs: Boolean? = null,
        val meldegruppe: String? = null
    )

    class Dag(
        val dato: LocalDate,
        val aktiviteter: List<Aktivitet> = emptyList(),
        val dagIndex: Int
    ) {
        fun finnesAktivitetMedType(aktivitetsType: Aktivitet.AktivitetsType): Boolean {
            return this.aktiviteter.find { aktivitet -> aktivitet.type == aktivitetsType } != null
        }

        fun hentArbeidstimer(): Double {
            return this.aktiviteter.find { aktivitet -> aktivitet.type == Aktivitet.AktivitetsType.Arbeid }?.timer
                ?: 0.0
        }
    }

    private fun mapAktivitetsdager(fom: LocalDate): List<Dag> {
        val aktivitetsdager = List(14) { index ->
            Dag(fom.plusDays(index.toLong()), mutableListOf(), index)
        }
        this.sporsmal?.meldekortDager?.forEach { dag ->
            if (dag.arbeidetTimerSum != null && dag.arbeidetTimerSum > 0) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Arbeid,
                        dag.arbeidetTimerSum.toDouble()
                    )
                )
            }
            if (dag.syk == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Syk,
                        null
                    )
                )
            }
            if (dag.kurs == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Utdanning,
                        null
                    )
                )
            }
            if (dag.annetFravaer == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Fravaer,
                        null
                    )
                )
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