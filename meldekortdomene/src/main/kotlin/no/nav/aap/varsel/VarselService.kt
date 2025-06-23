package no.nav.aap.varsel

import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Fagsaknummer
import java.time.Instant
import java.util.*

class VarselService(
    private val varselRepository: VarselRepository,
    private val varselGateway: VarselGateway
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        varselRepository = repositoryProvider.provide(),
        varselGateway = gatewayProvider.provide()
    )

    fun planleggFremtidigVarsel(fagsaknummer: Fagsaknummer) {
        val andrePlanlagteVarsler = varselRepository.hentVarsler(fagsaknummer)
            .filter { it.status == VarselStatus.PLANLAGT }

        // TODO planlegge neste vs planlegge alle varsler frem i tid?:
        if (andrePlanlagteVarsler.isNotEmpty()) {
            // Avbryt/slett andrePlanlagteVarsler?
        }

        /*
        Planlegge neste varsel:
            - Før vedtak:
                - Varsel: Kan sende meldekort
            - Etter vedtak:
                - Før første meldepliktperiode: Oppgave - Bør sende meldekort for opplysningsplikt
                - Neste meldepliktperiode: Oppgave - Må sende meldekort for meldeplikt
         */
        val sendingstidspunkt = Instant.now() // TODO
        val typeVarsel = TypeVarsel.OPPGAVE // TODO
        val typeVarselTekst = TypeVarselTekst.MELDEPLIKTPERIODE // TODO
        val varsel = Varsel(
            varselId = VarselId(UUID.randomUUID()),
            typeVarsel = typeVarsel,
            typeVarselTekst = typeVarselTekst,
            fagsaknummer = fagsaknummer,
            sendingstidspunkt = sendingstidspunkt,
            status = VarselStatus.PLANLAGT,
        )

        varselRepository.upsert(varsel)
    }

    fun sendPlanlagteVarsler() {
        varselRepository.hentVarslerForUtsending().forEach { varsel ->
            sendVarselOgPlanleggNeste(varsel)
        }
    }

    private fun sendVarselOgPlanleggNeste(varsel: Varsel) {
        // TODO inaktiver sendte (aktive) varsler?:
        varselRepository.hentVarsler(varsel.fagsaknummer)
            .filter { it.status == VarselStatus.SENDT && it.typeVarsel == TypeVarsel.OPPGAVE }
            .forEach { varsel ->
                varselRepository.upsert(varsel.copy(status = VarselStatus.INAKTIVERT))
                varselGateway.inaktiverVarsel(varsel)
            }

        varselRepository.upsert(varsel.copy(status = VarselStatus.SENDT))

        val brukerId =
            TODO() // slå opp vilkårlig ident for sak fra kelvin_person_ident og hent gjeldende ident fra PDL?
        val varselTekster = when (varsel.typeVarselTekst) {
            TypeVarselTekst.VALGFRITT_OPPLYSNINGSBEHOV -> TEKSTER_BESKJED_FREMTIDIG_OPPLYSNINGSBEHOV
            TypeVarselTekst.OPPLYSNINGSBEHOV -> TEKSTER_OPPGAVE_OPPLYSNINGSBEHOV
            TypeVarselTekst.MELDEPLIKTPERIODE -> TEKSTER_OPPGAVE_MELDEPLIKTPERIODE
        }
        varselGateway.sendVarsel(brukerId = brukerId, varsel = varsel, varselTekster = varselTekster)

        planleggFremtidigVarsel(varsel.fagsaknummer)
    }
}
