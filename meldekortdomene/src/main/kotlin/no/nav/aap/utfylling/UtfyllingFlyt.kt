package no.nav.aap.utfylling

import no.nav.aap.arena.ArenaService
import no.nav.aap.journalføring.BestillJournalføringSteg
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.PersisterOpplysningerSteg
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
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

class Utfyllingsflyter(
    arenaService: ArenaService,
    timerArbeidetRepository: TimerArbeidetRepository,
) {
    private val arenaVanligFlyt = ArenaVanligFlyt(arenaService, timerArbeidetRepository)
    private val arenaKorrigeringFlyt = ArenaKorrigeringFlyt(arenaService, timerArbeidetRepository)
    private val aapFlyt = AapFlyt(timerArbeidetRepository)

    fun flytForNavn(flytNavn: UtfyllingFlytNavn): UtfyllingFlyt {
        return when (flytNavn) {
            UtfyllingFlytNavn.ARENA_VANLIG_FLYT -> arenaVanligFlyt
            UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT -> arenaKorrigeringFlyt
            UtfyllingFlytNavn.AAP_FLYT -> aapFlyt
        }
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak): Utfyllingsflyter {
            return Utfyllingsflyter(
                arenaService = ArenaService.konstruer(connection, sak),
                timerArbeidetRepository = RepositoryProvider(connection).provide(),
            )
        }
    }
}

class ArenaVanligFlyt(
    arenaService: ArenaService,
    timerArbeidetRepository: TimerArbeidetRepository,
) : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.ARENA_VANLIG_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        ArenaKontrollVanligSteg(arenaService),
        PersisterOpplysningerSteg(timerArbeidetRepository),
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
    timerArbeidetRepository: TimerArbeidetRepository,
) : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        ArenaKontrollKorrigeringSteg(arenaService),
        PersisterOpplysningerSteg(timerArbeidetRepository),
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

class AapFlyt(
    timerArbeidetRepository: TimerArbeidetRepository,
) : UtfyllingFlyt {
    override val navn = UtfyllingFlytNavn.AAP_FLYT

    override val steg = listOf(
        IntroduksjonSteg,
        SpørsmålSteg,
        TimerArbeidetSteg,
        StemmerOpplysningeneSteg,
        PersisterOpplysningerSteg(timerArbeidetRepository),
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