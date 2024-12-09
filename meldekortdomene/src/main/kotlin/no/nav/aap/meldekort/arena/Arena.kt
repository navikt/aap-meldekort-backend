package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import java.time.LocalDate

interface Arena {
    fun meldegrupper(innloggetBruker: InnloggetBruker): List<Meldegruppe>

    fun person(innloggetBruker: InnloggetBruker): Person?

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): Person

    fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: Long): Meldekortdetaljer

    fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long

    fun sendInn(innloggetBruker: InnloggetBruker, request: MeldekortkontrollRequest): MeldekortkontrollResponse

    data class Meldegruppe(
        val fodselsnr: String,
        val meldegruppeKode: String,
        val datoFra: LocalDate,
        val datoTil: LocalDate? = null,
        val hendelsesdato: LocalDate,
        val statusAktiv: String,
        val begrunnelse: String,
        val styrendeVedtakId: Long? = null
    )

    data class Person(
        val personId: Long,
        val etternavn: String,
        val fornavn: String,
        val maalformkode: String,
        val meldeform: String,
        val meldekortListe: List<Meldekort>? = null,
    )

    data class Meldekort(
        val meldekortId: Long,
        val kortType: String,
        val meldeperiode: String,
        val fraDato: LocalDate,
        val tilDato: LocalDate,
        val hoyesteMeldegruppe: String,
        val beregningstatus: String,
        val forskudd: Boolean,
        val mottattDato: LocalDate? = null,
        val bruttoBelop: Float = 0F
    )

    data class Meldekortdetaljer(
        val id: String,
        val personId: Long,
        val fodselsnr: String,
        val meldekortId: Long,
        val meldeperiode: String,
        val meldegruppe: String,
        val arkivnokkel: String,
        val kortType: String,
        val meldeDato: LocalDate? = null,
        val lestDato: LocalDate? = null,
        val sporsmal: Sporsmal? = null,
        val begrunnelse: String? = ""
    )

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

    data class MeldekortkontrollRequest(
        var meldekortId: Long = 0,
        var fnr: String,
        var personId: Long = 0,
        var kilde: String,
        var kortType: String,
        val meldedato: LocalDate,
        val periodeFra: LocalDate,
        val periodeTil: LocalDate,
        var meldegruppe: String,
        var annetFravaer: Boolean,
        var arbeidet: Boolean,
        var arbeidssoker: Boolean,
        var kurs: Boolean,
        var syk: Boolean,
        var begrunnelse: String?,
        var meldekortdager: List<MeldekortkontrollFravaer>
    )

    data class MeldekortkontrollFravaer(
        val dato: LocalDate,
        val syk: Boolean,
        val kurs: Boolean,
        val annetFravaer: Boolean,
        val arbeidTimer: Double
    )

    data class MeldekortkontrollResponse(
        var meldekortId: Long = 0,
        var kontrollStatus: String = "",
        var feilListe: List<MeldekortkontrollFeil> = emptyList(),
        var oppfolgingListe: List<MeldekortkontrollFeil> = emptyList()
    )

    data class MeldekortkontrollFeil(
        var kode: String,
        var params: List<String>? = null
    )
}