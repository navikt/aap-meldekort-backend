package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.Arena.KortStatus.OPPRE
import no.nav.aap.meldekort.arena.Arena.KortStatus.SENDT
import no.nav.aap.meldekort.arena.Arena.KortStatus.UBEHA
import no.nav.aap.meldekort.arena.Arena.KortType.KORRIGERT_ELEKTRONISK
import no.nav.aap.meldekort.arena.Meldeperiode.Type.ETTERREGISTRERT
import no.nav.aap.meldekort.arena.Meldeperiode.Type.ORDINÆRT
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

        val kortType: KortType,
        val meldeperiode: String,
        val fraDato: LocalDate,
        val tilDato: LocalDate,
        val hoyesteMeldegruppe: String,

        val beregningstatus: KortStatus,
        val forskudd: Boolean,
        val mottattDato: LocalDate? = null,
        val bruttoBelop: Float = 0F
    ) {

        fun tilMeldeperiodeHvisRelevant(meldekortListe: List<Meldekort>): Meldeperiode? {
            if (hoyesteMeldegruppe != AAP_KODE) return null
            val type = type() ?: return null

            val kanSendesFra = tilDato.minusDays(1)
            return Meldeperiode(
                meldekortId = meldekortId,
                periode = Periode(fraDato, tilDato),
                kanSendesFra = kanSendesFra,
                kanSendes = !LocalDate.now().isBefore(kanSendesFra),
                kanEndres = kanEndres(this, meldekortListe),
                type = type,
            )
        }

        private fun kanEndres(meldekort: Meldekort, meldekortListe: List<Meldekort>): Boolean {
            return meldekort.kortType != KORRIGERT_ELEKTRONISK &&
                    meldekort.beregningstatus != UBEHA &&
                    meldekortListe.none { mk ->
                        meldekort.meldekortId != mk.meldekortId &&
                                meldekort.meldeperiode == mk.meldeperiode &&
                                mk.kortType == KORRIGERT_ELEKTRONISK
                    }
        }

        private fun type(): Meldeperiode.Type? {
            return when {
                kortType != KortType.MANUELL_ARENA && beregningstatus in arrayOf(OPPRE, SENDT) -> ORDINÆRT
                kortType == KortType.MANUELL_ARENA && beregningstatus == OPPRE -> ETTERREGISTRERT
                else -> null
            }
        }
    }

    data class Meldekortdetaljer(
        val id: String,
        val personId: Long,
        val fodselsnr: String,
        val meldekortId: Long,
        val meldeperiode: String,
        val meldegruppe: String,
        val arkivnokkel: String,
        val kortType: KortType,
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
        val kortType: KortType,
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

        companion object {
            fun getByCode(code: String): KortType {
                return requireNotNull(entries.find { it.code == code }) {
                    "ukjent KortType $code fra arena"
                }
            }
        }
    }
}