package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import java.time.LocalDate
import java.util.HashMap

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

        /**
         * 05 Elektronisk kort
         * 06 Automatisk kort
         * 07 Manuelt kort
         * 08 Maskinelt oppdatert kort
         * 09 Manuelt kort - opprettet av saksbehandler eller kort som opprettes tilbake i tid
         *    Brukes både ved korrigering og etterregistrering. Ved etterregistrering kan
         *    kortet fylles ut av saksbehandler i Arena eller bruker på nav.no.
         * 10 Elektronisk kort - korrigert av bruker
         *    En type meldekort som kun kan opprettes/intieres av bruker på nav.no, og som alltid
         *    vil være en korrigering av et eksisterende innlevert kort.
         */
        val kortType: KortType,
        val meldeperiode: String,
        val fraDato: LocalDate,
        val tilDato: LocalDate,
        val hoyesteMeldegruppe: String,

        /**
         * OPPRE Kortet er opprettet for en periode LAV Kortet er opprettet
         * SENDT Kortet er sendt til print (kun papir) LAV Kortet er sendt ut
         * REGIS Kortet er mottatt fra Amelding (kun temporær status) HØY Til behandling
         * FMOPP Kortet har feilet i Amelding og det er sendt til oppfølging i Arena HØY Til manuell saksbehandling
         * FUOPP Kortet har feilet i Amelding, og returkort er sendt til arbeidssøker HØY Til behandling *)
         * KLAR Kortet er OK i Amelding, personen har ytelse i perioden og kortet er klar til beregning HØY Klar til beregning
         * KAND Person har ikke vedtak om ytelser men meldegruppen tilsier at det vil fattes et vedtak HØY Klar til beregning
         * IKKE Kortet skal ikke beregnes siden personen ikke har vedtak eller at et annet kort er beregnet for hele vedtaksperioden i samme meldeperiode HØY Ingen beregning
         * OVERM Et annet kort for samme periode har overstyrt dette kortet HØY Ingen beregning
         * NYKTR Kortet er kontrollert etter for lav meldegruppe og er sendt til ny kontroll i Amelding HØY Til behandling
         * FERDI Kortet er ferdig beregnet HØY Ferdig beregnet
         * FEIL Kortet har feilet i beregning HØY Til manuell saksbehandling
         * VENTE Kortet feilet i beregning fordi forrige kort mangler. HØY Venter på behandling av tidligere meldekort
         * SLETT Kortet er slettet <Ingen> Vises ikke
         */
        val beregningstatus: KortStatus,
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
        val meldekortId: Long,
        val fnr: String,
        val personId: Long,
        val kortType: String,
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
    )

    data class MeldekortkontrollFravaer(
        val dato: LocalDate,
        val arbeidTimer: Double,
        val syk: Boolean = false,
        val kurs: Boolean = false,
        val annetFravaer: Boolean = false,
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

    enum class KortStatus {
        OPPRE,
        SENDT,
        SLETT,
        REGIS,
        FMOPP,
        FUOPP,
        KLAR,
        KAND,
        IKKE,
        OVERM,
        NYKTR,
        FERDI,
        FEIL,
        VENTE,
        OPPF,
        UBEHA  // OBS: "Fiktiv" status som benyttes for meldekort som er klare for innlesing til Arena
    }

    enum class KortType(val code: String) {
        ORDINAER("01"),
        ERSTATNING("03"),
        RETUR("04"),
        ELEKTRONISK("05"),
        AAP("06"),
        ORDINAER_MANUELL("07"),
        MASKINELT_OPPDATERT("08"),
        MANUELL_ARENA("09"),
        KORRIGERT_ELEKTRONISK("10");

        override fun toString(): String {
            return code
        }

        companion object {
            private var values: MutableMap<String, KortType>? = null

            init {
                values = HashMap()
                for (kortType in values()) {
                    values!![kortType.code] = kortType
                }
            }

            fun getByCode(code: String): KortType {
                val kortType = values!![code]
                return kortType ?: throw IllegalArgumentException("Ugyldig kode [ $code ]")
            }
        }
    }
}