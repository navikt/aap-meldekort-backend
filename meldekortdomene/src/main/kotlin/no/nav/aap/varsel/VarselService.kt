package no.nav.aap.varsel

import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Fagsaknummer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.*

class VarselService(
    private val kelvinSakRepository: KelvinSakRepository,
    private val varselRepository: VarselRepository,
    private val varselGateway: VarselGateway,
    private val clock: Clock
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakRepository = repositoryProvider.provide(),
        varselRepository = repositoryProvider.provide(),
        varselGateway = gatewayProvider.provide(),
        clock = clock
    )

    fun planleggFremtidigeVarsler(fagsaknummer: Fagsaknummer) {
        val meldeplikt =
            kelvinSakRepository.hentMeldeplikt(fagsaknummer)
                .filter { it.contains(LocalDate.now(clock)) }


        val varsler = meldeplikt.map {
            lagVarsel(fagsaknummer, it, TypeVarselOm.MELDEPLIKTPERIODE)
        }

        // Vurder å ikke slette de som overlapper med nye (beholder opprettet)
        varselRepository.slettPlanlagteVarsler(fagsaknummer, TypeVarselOm.MELDEPLIKTPERIODE)
        varsler.forEach { varsel ->
            varselRepository.upsert(varsel)
        }
    }

    fun lagVarsel(
        fagsaknummer: Fagsaknummer,
        periode: Periode,
        varselOm: TypeVarselOm
    ): Varsel {
        return Varsel(
            varselId = VarselId(UUID.randomUUID()),
            typeVarsel = utledTypeVarsel(varselOm),
            typeVarselOm = varselOm,
            fagsaknummer = fagsaknummer,
            sendingstidspunkt = Instant.from(periode.fom), // TODO velge utsendingstidspunkt?
            status = VarselStatus.PLANLAGT,
            forPeriode = periode,
            opprettet = Instant.now(clock),
            sistEndret = Instant.now(clock)
        )
    }

    private fun utledTypeVarsel(varselOm: TypeVarselOm): TypeVarsel {
        return when (varselOm) {
            TypeVarselOm.VALGFRITT_OPPLYSNINGSBEHOV -> TypeVarsel.BESKJED
            TypeVarselOm.OPPLYSNINGSBEHOV, TypeVarselOm.MELDEPLIKTPERIODE -> TypeVarsel.OPPGAVE
        }
    }

    fun sendPlanlagteVarsler() {
        varselRepository.hentVarslerForUtsending().forEach { varsel ->
            sendVarsel(varsel)
        }
    }

    private fun sendVarsel(varsel: Varsel) {
        varselRepository.upsert(varsel.copy(status = VarselStatus.SENDT))

        // TODO hent gjeldende ident fra PDL
        val brukerId = kelvinSakRepository.hentIdenter(varsel.fagsaknummer).first()
        val varselTekster = when (varsel.typeVarselOm) {
            TypeVarselOm.VALGFRITT_OPPLYSNINGSBEHOV -> TEKSTER_BESKJED_FREMTIDIG_OPPLYSNINGSBEHOV
            TypeVarselOm.OPPLYSNINGSBEHOV -> TEKSTER_OPPGAVE_OPPLYSNINGSBEHOV
            TypeVarselOm.MELDEPLIKTPERIODE -> TEKSTER_OPPGAVE_MELDEPLIKTPERIODE
        }
        varselGateway.sendVarsel(brukerId = brukerId, varsel = varsel, varselTekster = varselTekster)
    }
}
