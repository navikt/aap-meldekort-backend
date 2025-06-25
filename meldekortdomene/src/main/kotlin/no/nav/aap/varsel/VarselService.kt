package no.nav.aap.varsel

import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.kelvin.KelvinSakService
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.Meldeperiode
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.Utfylling
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class VarselService(
    private val kelvinSakService: KelvinSakService,
    private val kelvinSakRepository: KelvinSakRepository,
    private val varselRepository: VarselRepository,
    private val varselGateway: VarselGateway,
    private val clock: Clock
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakService = KelvinSakService(repositoryProvider, gatewayProvider, clock),
        kelvinSakRepository = repositoryProvider.provide(),
        varselRepository = repositoryProvider.provide(),
        varselGateway = gatewayProvider.provide(),
        clock = clock
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun planleggFremtidigeVarsler(saksnummer: Fagsaknummer) {
        val meldeplikt =
            kelvinSakRepository.hentMeldeplikt(saksnummer)
                .filter { it.fom.isAfter(LocalDate.now(clock)) }

        val meldelerioder = kelvinSakService.hentMeldeperioder(FagsakReferanse(FagsystemNavn.KELVIN, saksnummer))
            .filter { meldeplikt.contains(it.meldevindu) }

        if (meldelerioder.size != meldeplikt.size) {
            log.warn("Meldevindu i meldeperioder samsvarer ikke med fremtidige meldeplikt-perioder fra Kelvin")
        }

        val varsler = meldelerioder.map {
            lagVarsel(saksnummer, it, TypeVarselOm.MELDEPLIKTPERIODE)
        }

        varselRepository.slettPlanlagteVarsler(saksnummer, TypeVarselOm.MELDEPLIKTPERIODE)
        varsler.forEach { varsel ->
            varselRepository.upsert(varsel)
        }
    }

    fun lagVarsel(
        fagsaknummer: Fagsaknummer,
        meldeperiode: Meldeperiode,
        varselOm: TypeVarselOm
    ): Varsel {
        return Varsel(
            varselId = VarselId(UUID.randomUUID()),
            typeVarsel = utledTypeVarsel(varselOm),
            typeVarselOm = varselOm,
            saksnummer = fagsaknummer,
            // TODO hvilket utsendingstidspunkt?
            sendingstidspunkt = meldeperiode.meldevindu.fom.atTime(LocalTime.of(9, 0)).atZone(ZoneId.systemDefault())
                .toInstant(),
            status = VarselStatus.PLANLAGT,
            forPeriode = meldeperiode.meldeperioden,
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
        varselRepository.hentVarslerForUtsending(clock)
            .also {
                if (it.isNotEmpty()) {
                    log.info("Sender ${it.size} varsler")
                }
            }
            .forEach { varsel ->
                sendVarsel(varsel)
            }
    }

    private fun sendVarsel(varsel: Varsel) {
        varselRepository.upsert(varsel.copy(status = VarselStatus.SENDT))

        // TODO hent gjeldende ident fra PDL
        val brukerId = kelvinSakRepository.hentIdenter(varsel.saksnummer).first()
        val varselTekster = when (varsel.typeVarselOm) {
            TypeVarselOm.VALGFRITT_OPPLYSNINGSBEHOV -> TEKSTER_BESKJED_FREMTIDIG_OPPLYSNINGSBEHOV
            TypeVarselOm.OPPLYSNINGSBEHOV -> TEKSTER_OPPGAVE_OPPLYSNINGSBEHOV
            TypeVarselOm.MELDEPLIKTPERIODE -> TEKSTER_OPPGAVE_MELDEPLIKTPERIODE
        }
        varselGateway.sendVarsel(brukerId = brukerId, varsel = varsel, varselTekster = varselTekster)
    }

    fun inaktiverVarsel(utfylling: Utfylling) {
        if (utfylling.fagsak.system != FagsystemNavn.KELVIN) return

        val varsel = varselRepository.hentVarsler(utfylling.fagsak.nummer)
            .singleOrNull {
                it.status == VarselStatus.SENDT &&
                        it.typeVarsel == TypeVarsel.OPPGAVE &&
                        it.forPeriode == utfylling.periode
            }

        if (varsel != null) {
            log.info("Inaktiverer varsel med varsel id ${varsel.varselId}")
            varselRepository.upsert(varsel.copy(status = VarselStatus.INAKTIVERT))
            varselGateway.inaktiverVarsel(varsel)
        } else {
            log.info("Fant ikke varsel å inaktivere for utfylling periode ${utfylling.periode}")
        }
    }
}
