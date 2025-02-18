package no.nav.aap.utfylling

import no.nav.aap.arena.ArenaService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.sak.Sak

interface UtfyllingFlyt {
    val navn: UtfyllingFlytNavn
    val steg: List<UtfyllingSteg>

    fun stegForNavn(steg: UtfyllingStegNavn): UtfyllingSteg {
        return this.steg.find { it.navn == steg }!!
    }
}

enum class UtfyllingFlytNavn {
    ARENA_VANLIG_FLYT,
    ARENA_KORRIGERING_FLYT,
    AAP_FLYT,
}

class Utfyllingsflyter(arenaService: ArenaService) {
    private val arenaVanligFlyt = ArenaVanligFlyt(arenaService)
    private val arenaKorrigeringFlyt = ArenaKorrigeringFlyt(arenaService)
    private val aapFlyt = AapFlyt

    fun flytForNavn(flytNavn: UtfyllingFlytNavn): UtfyllingFlyt {
        return when (flytNavn) {
            UtfyllingFlytNavn.ARENA_VANLIG_FLYT -> arenaVanligFlyt
            UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT -> arenaKorrigeringFlyt
            UtfyllingFlytNavn.AAP_FLYT -> aapFlyt
        }
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): Utfyllingsflyter {
            return Utfyllingsflyter(ArenaService.konstruer(connection, sak))
        }
    }
}

class ArenaVanligFlyt(
    arenaService: ArenaService,
) : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.ARENA_VANLIG_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        ArenaKontrollVanligSteg(arenaService),
        PersisterOpplysningerSteg(),
        BestillJournalføringSteg(),
        KvitteringSteg,
    )

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingFlyt && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

class ArenaKorrigeringFlyt(
    arenaService: ArenaService,
) : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        ArenaKontrollKorrigeringSteg(arenaService),
        PersisterOpplysningerSteg(),
        BestillJournalføringSteg(),
        KvitteringSteg,
    )

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingFlyt && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}

object AapFlyt : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.AAP_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        PersisterOpplysningerSteg(),
        BestillJournalføringSteg(),
        KvitteringSteg,
    )

    override fun equals(other: Any?): Boolean {
        return other is UtfyllingFlyt && this.navn == other.navn
    }

    override fun hashCode(): Int {
        return navn.hashCode()
    }
}