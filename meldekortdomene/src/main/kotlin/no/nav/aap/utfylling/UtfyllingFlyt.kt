package no.nav.aap.utfylling

import no.nav.aap.InnloggetBruker
import no.nav.aap.journalføring.BestillJournalføringSteg
import no.nav.aap.journalføring.JournalføringService
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.opplysningsplikt.PersisterOpplysningerSteg
import no.nav.aap.utfylling.UtfyllingStegNavn.BEKREFT
import no.nav.aap.utfylling.UtfyllingStegNavn.BESTILL_JOURNALFØRING
import no.nav.aap.utfylling.UtfyllingStegNavn.INTRODUKSJON
import no.nav.aap.utfylling.UtfyllingStegNavn.KVITTERING
import no.nav.aap.utfylling.UtfyllingStegNavn.PERSISTER_OPPLYSNINGER
import no.nav.aap.utfylling.UtfyllingStegNavn.SPØRSMÅL
import no.nav.aap.utfylling.UtfyllingStegNavn.UTFYLLING
import no.nav.aap.utfylling.UtfyllingStegNavn.FRAVÆR_SPØRSMÅL
import no.nav.aap.utfylling.UtfyllingStegNavn.FRAVÆR_UTFYLLING
import no.nav.aap.utfylling.UtfyllingStegNavn.INAKTIVER_VARSEL
import no.nav.aap.varsel.InaktiverVarselSteg
import no.nav.aap.varsel.VarselService
import org.slf4j.LoggerFactory
import java.time.Clock

enum class UtfyllingFlytNavn(
    val steg: List<UtfyllingStegNavn>,
) {
    AAP_FLYT(
        listOf(
            INTRODUKSJON,
            SPØRSMÅL,
            UTFYLLING,
            FRAVÆR_SPØRSMÅL,
            FRAVÆR_UTFYLLING,
            BEKREFT,
            PERSISTER_OPPLYSNINGER,
            BESTILL_JOURNALFØRING,
            INAKTIVER_VARSEL,
            KVITTERING,
        )
    ),
    AAP_KORRIGERING_FLYT(
        listOf(
            SPØRSMÅL,
            UTFYLLING,
            FRAVÆR_SPØRSMÅL,
            FRAVÆR_UTFYLLING,
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
        check(!utfylling.erAvsluttet) { "Utfylling er allerede avsluttet"}

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
        fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
            flytNavn: UtfyllingFlytNavn,
            clock: Clock,
        ): UtfyllingFlyt {
            return UtfyllingFlyt(
                stegene = flytNavn.steg.map {
                    when (it) {
                        INTRODUKSJON -> IntroduksjonSteg
                        SPØRSMÅL -> AapSpørsmålSteg
                        UTFYLLING -> TimerArbeidetSteg
                        FRAVÆR_SPØRSMÅL -> FraværSpørsmålSteg
                        FRAVÆR_UTFYLLING -> DagerFraværSteg
                        BEKREFT -> StemmerOpplysningeneSteg(clock)
                        PERSISTER_OPPLYSNINGER -> PersisterOpplysningerSteg(repositoryProvider.provide())
                        BESTILL_JOURNALFØRING -> BestillJournalføringSteg(JournalføringService(repositoryProvider, gatewayProvider))
                        INAKTIVER_VARSEL -> InaktiverVarselSteg(VarselService(repositoryProvider, gatewayProvider, clock))
                        KVITTERING -> KvitteringSteg
                    }
                }
            )
        }
    }
}
