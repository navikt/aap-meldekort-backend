package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.arena.ArenaClient.KortType.KORRIGERT_ELEKTRONISK
import no.nav.aap.meldekort.arena.ArenaMeldekort.KortStatus.OPPRE
import no.nav.aap.meldekort.arena.ArenaMeldekort.KortStatus.SENDT
import no.nav.aap.meldekort.arena.ArenaMeldekort.KortStatus.UBEHA
import no.nav.aap.meldekort.arenaflyt.Meldeperiode
import no.nav.aap.meldekort.arenaflyt.Meldeperiode.Type.ETTERREGISTRERT
import no.nav.aap.meldekort.arenaflyt.Meldeperiode.Type.ORDINÆRT
import no.nav.aap.meldekort.arenaflyt.Periode
import java.time.LocalDate

data class ArenaMeldekort(
    val meldekortId: Long,

    val kortType: ArenaClient.KortType,
    val meldeperiode: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val hoyesteMeldegruppe: String,

    val beregningstatus: KortStatus,
    val forskudd: Boolean,
    val mottattDato: LocalDate? = null,
    val bruttoBelop: Float = 0F,

    val historisk: Boolean,
) {

    fun tilMeldeperiodeHvisRelevant(arenaMeldekortListe: List<ArenaMeldekort>): Meldeperiode? {
        if (hoyesteMeldegruppe != AAP_KODE) return null
        val type = type() ?: return null

        val kanSendesFra = tilDato.minusDays(1)
        return Meldeperiode(
            meldekortId = meldekortId,
            periode = Periode(fraDato, tilDato),
            kanSendesFra = kanSendesFra,
            kanSendes = !LocalDate.now().isBefore(kanSendesFra),
            kanEndres = kanEndres(this, arenaMeldekortListe),
            type = type,
        )
    }

    private fun kanEndres(arenaMeldekort: ArenaMeldekort, arenaMeldekortListe: List<ArenaMeldekort>): Boolean {
        return arenaMeldekort.kortType != KORRIGERT_ELEKTRONISK &&
                arenaMeldekort.beregningstatus != UBEHA &&
                arenaMeldekortListe.none { mk ->
                    arenaMeldekort.meldekortId != mk.meldekortId &&
                            arenaMeldekort.meldeperiode == mk.meldeperiode &&
                            mk.kortType == KORRIGERT_ELEKTRONISK
                }
    }

    private fun type(): Meldeperiode.Type? {
        return when {
            kortType != ArenaClient.KortType.MANUELL_ARENA && beregningstatus in arrayOf(OPPRE, SENDT) -> ORDINÆRT
            kortType == ArenaClient.KortType.MANUELL_ARENA && beregningstatus == OPPRE -> ETTERREGISTRERT
            else -> null
        }
    }

    companion object {
        fun List<ArenaMeldekort>.tilMeldeperioder(): List<Meldeperiode> {
            return this
                .filterNot { it.historisk }
                .mapNotNull { it.tilMeldeperiodeHvisRelevant(this) }
        }
    }

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
}