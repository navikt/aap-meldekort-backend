package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.arena.ArenaClient.KortType.KORRIGERT_ELEKTRONISK
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.UBEHA
import no.nav.aap.meldekort.arena.MeldekortStatus.FEILET
import no.nav.aap.meldekort.arena.MeldekortStatus.FERDIG
import no.nav.aap.meldekort.arena.MeldekortStatus.INNSENDT
import no.nav.aap.meldekort.arena.MeldekortStatus.OVERSTYRT_AV_ANNET_MELDEKORT
import java.time.LocalDate
import java.time.temporal.WeekFields

data class MeldekortId(val asLong: Long)

data class ArenaMeldekort(
    val meldekortId: MeldekortId,

    val kortType: ArenaClient.KortType,
    val meldeperiode: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val hoyesteMeldegruppe: String,

    val beregningstatus: ArenaStatus,
    val forskudd: Boolean,
    val mottattDato: LocalDate? = null,
    val bruttoBelop: Double?,
) {
    val erForAap: Boolean = hoyesteMeldegruppe == AAP_KODE

    val periodekode: String = run {
        val år = fraDato.get(WeekFields.ISO.weekBasedYear())
        val uke = fraDato.get(WeekFields.ISO.weekOfWeekBasedYear()).toString().padStart(2, '0')
        "$år$uke"
    }

    val type: MeldekortType = kortType.meldekortType

    fun erUbehandletEllerKorrigeringForPerioden(arenaMeldekortListe: List<ArenaMeldekort>): Boolean {
        return kortType == KORRIGERT_ELEKTRONISK ||
                beregningstatus == UBEHA ||
                arenaMeldekortListe.any { mk ->
                    meldekortId != mk.meldekortId &&
                            meldeperiode == mk.meldeperiode &&
                            mk.kortType == KORRIGERT_ELEKTRONISK
                }
    }


    enum class ArenaStatus(
        val historiskStatus: MeldekortStatus,
    ) {
        /** Kortet er opprettet for en periode. */
        OPPRE(INNSENDT),

        /** Kortet er sendt til print (kun papir). */
        SENDT(INNSENDT),

        /** Kortet er slettet. */
        SLETT(INNSENDT),

        /** Kortet er mottatt fra Amelding (kun temporær status). */
        REGIS(INNSENDT),

        /** Kortet har feilet i Amelding og det er sendt til oppfølging i Arena */
        FMOPP(INNSENDT),

        /** Kortet har feilet i Amelding, og returkort er sendt til arbeidssøker. */
        FUOPP(INNSENDT),

        /** Kortet er OK i Amelding, personen har ytelse i perioden og kortet er klar til beregning. */
        KLAR(INNSENDT),

        /** Person har ikke vedtak om ytelser men meldegruppen tilsier at det vil fattes et vedtak. */
        KAND(INNSENDT),

        /** Kortet skal ikke beregnes siden personen ikke har vedtak eller at et annet kort er
         *  beregnet for hele vedtaksperioden i samme meldeperiode. */
        IKKE(FERDIG),

        /** Et annet kort for samme periode har overstyrt dette kortet. */
        OVERM(OVERSTYRT_AV_ANNET_MELDEKORT),

        /** Kortet er kontrollert etter for lav meldegruppe og er sendt til ny kontroll i Amelding. */
        NYKTR(INNSENDT),

        /** Kortet er ferdig beregnet. */
        FERDI(FERDIG),

        /** Kortet har feilet i beregning. */
        FEIL(FEILET),

        /** Kortet feilet i beregning fordi forrige kort mangler. */
        VENTE(INNSENDT),
        OPPF(INNSENDT),
        UBEHA(INNSENDT)  // OBS: "Fiktiv" status som benyttes for meldekort som er klare for innlesing til Arena
    }
}