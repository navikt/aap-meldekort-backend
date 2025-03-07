package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.arena.ArenaSakService
import no.nav.aap.arena.MeldekortService
import no.nav.aap.journalføring.BestillJournalføringSteg
import no.nav.aap.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.PersisterOpplysningerSteg
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_KORRIGERING
import no.nav.aap.utfylling.UtfyllingStegNavn.ARENAKONTROLL_VANLIG
import no.nav.aap.utfylling.UtfyllingStegNavn.BEKREFT
import no.nav.aap.utfylling.UtfyllingStegNavn.BESTILL_JOURNALFØRING
import no.nav.aap.utfylling.UtfyllingStegNavn.INTRODUKSJON
import no.nav.aap.utfylling.UtfyllingStegNavn.KVITTERING
import no.nav.aap.utfylling.UtfyllingStegNavn.PERSISTER_OPPLYSNINGER
import no.nav.aap.utfylling.UtfyllingStegNavn.SPØRSMÅL
import no.nav.aap.utfylling.UtfyllingStegNavn.UTFYLLING
import org.slf4j.LoggerFactory

enum class UtfyllingFlytNavn(
    val steg: List<UtfyllingStegNavn>,
) {
    ARENA_VANLIG_FLYT(
        listOf(
            INTRODUKSJON,
            SPØRSMÅL,
            UTFYLLING,
            BEKREFT,
            ARENAKONTROLL_VANLIG,
            PERSISTER_OPPLYSNINGER,
            BESTILL_JOURNALFØRING,
            KVITTERING,
        )
    ),
    ARENA_KORRIGERING_FLYT(
        listOf(
            INTRODUKSJON,
            SPØRSMÅL,
            UTFYLLING,
            BEKREFT,
            ARENAKONTROLL_KORRIGERING,
            PERSISTER_OPPLYSNINGER,
            BESTILL_JOURNALFØRING,
            KVITTERING,
        )
    ),
    AAP_FLYT(
        listOf(
            INTRODUKSJON,
            SPØRSMÅL,
            UTFYLLING,
            BEKREFT,
            PERSISTER_OPPLYSNINGER,
            BESTILL_JOURNALFØRING,
            KVITTERING,
        )
    ),
}

class UtfyllingFlyt(
    private val stegene: List<UtfyllingSteg>,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    class FlytResultat(
        val utfylling: Utfylling,
        val feil: Exception? = null,
    )

    fun kjør(innloggetBruker: InnloggetBruker, utfylling: Utfylling): FlytResultat {
        check(stegene.any { it.navn == utfylling.aktivtSteg })
        check(!utfylling.erAvsluttet)

        val formkravFeilerSteg = oppfyllerFormkrav(utfylling)
        if (formkravFeilerSteg != null) {
            log.info("utfylling oppfyller ikke formkrav for steget $formkravFeilerSteg, så setter det som aktivt steg")
            return FlytResultat(utfylling.copy(aktivtSteg = formkravFeilerSteg))
        }

        val nesteAktiveSteg = nesteIkketekniskeSteg(utfylling)
        log.info("neste ikke-tekniske relevante steg er $nesteAktiveSteg")

        try {
            utførEffekter(innloggetBruker, utfylling, nesteAktiveSteg)
            log.info("alle effekter opp til $nesteAktiveSteg kjørte vellyket, aktivt steg blir $nesteAktiveSteg")
            return FlytResultat(utfylling.copy(aktivtSteg = nesteAktiveSteg))
        } catch (exception: Exception) {
            log.info("feil ved kjøring av effekter opp til $nesteAktiveSteg, aktivt steg uendret ${utfylling.aktivtSteg}")
            return FlytResultat(utfylling, exception)
        }
    }

    /** Sjekker formkrav fra starten og til og med aktivt steg.
     *
     * Hvis alle formkravene er oppfylt returneres `null`.
     * Om det formkrav ikke er oppfylt returneres første steg
     * hvor formkravet ikke er oppfylt.
     */
    private fun oppfyllerFormkrav(utfylling: Utfylling): UtfyllingStegNavn? {
        for (steg in stegene) {
            val feilendeFormkrav = steg.formkrav
                .filterNot { (_, formkravOppfylt) -> formkravOppfylt(utfylling) }
            if (feilendeFormkrav.isNotEmpty()) {
                log.info("formkrav {} feilet for steg {}",
                    feilendeFormkrav.entries.joinToString { (navn, _) -> navn },
                    steg.navn,
                )

                return steg.navn
            }

            /* ikke sjekk lenger frem enn det vi er. */
            if (steg.navn == utfylling.aktivtSteg) {
                break
            }
        }

        return null
    }

    private fun nesteIkketekniskeSteg(utfylling: Utfylling): UtfyllingStegNavn {
        var forbiAktivt = false

        for (steg in stegene) {
            if (forbiAktivt && !steg.erTeknisk && steg.erRelevant(utfylling)) {
                return steg.navn
            }
            if (steg.navn == utfylling.aktivtSteg) {
                forbiAktivt = true
            }
        }

        return stegene.last()
            .also {
                check(!it.erTeknisk)
                check(it.erRelevant(utfylling))
            }
            .navn
    }

    private fun utførEffekter(
        innloggetBruker: InnloggetBruker,
        utfylling: Utfylling,
        nesteAktiveSteg: UtfyllingStegNavn,
    ): Result<Unit> {
        for (steg in stegene) {
            if (steg.navn == nesteAktiveSteg) {
                break
            }

            try {
                steg.utførEffekt(innloggetBruker, utfylling)
            } catch (e: Exception) {
                return Result.failure(Exception("effekt for steg ${steg.navn} feilet", e))
            }
        }
        return Result.success(Unit)
    }

    companion object {
        fun konstruer(connection: DBConnection, sak: Sak, flytNavn: UtfyllingFlytNavn): UtfyllingFlyt {
            val repositoryProvider = RepositoryProvider(connection)

            val arenaSakService = lazy {
                val arenaGateway = GatewayProvider.provide<ArenaGateway>()
                ArenaSakService(
                    meldekortService = MeldekortService(
                        arenaGateway = arenaGateway,
                        meldekortRepository = repositoryProvider.provide(),
                    ),
                    arenaGateway = arenaGateway,
                    sak = sak,
                )
            }

            return UtfyllingFlyt(
                stegene = flytNavn.steg.map {
                    when (it) {
                        INTRODUKSJON -> IntroduksjonSteg
                        SPØRSMÅL -> SpørsmålSteg
                        UTFYLLING -> TimerArbeidetSteg
                        BEKREFT -> StemmerOpplysningeneSteg
                        ARENAKONTROLL_VANLIG -> ArenaKontrollVanligSteg(arenaSakService.value)
                        ARENAKONTROLL_KORRIGERING -> ArenaKontrollKorrigeringSteg(arenaSakService.value)
                        PERSISTER_OPPLYSNINGER -> PersisterOpplysningerSteg(repositoryProvider.provide())
                        BESTILL_JOURNALFØRING -> BestillJournalføringSteg(JournalføringService.konstruer(connection))
                        KVITTERING -> KvitteringSteg
                    }
                }
            )
        }
    }
}
