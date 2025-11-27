package no.nav.aap.varsel

import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.kelvin.KelvinSakService
import no.nav.aap.kelvin.originalInnsendingstidspunkt
import no.nav.aap.kelvin.tidligsteInnsendingstidspunkt
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
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.utfylling.UtfyllingRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.util.*

class VarselService(
    private val kelvinSakService: KelvinSakService,
    private val kelvinSakRepository: KelvinSakRepository,
    private val varselRepository: VarselRepository,
    private val utfyllingRepository: UtfyllingRepository,
    private val varselGateway: VarselGateway,
    private val clock: Clock
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, clock: Clock) : this(
        kelvinSakService = KelvinSakService(repositoryProvider, gatewayProvider, clock),
        kelvinSakRepository = repositoryProvider.provide(),
        varselRepository = repositoryProvider.provide(),
        utfyllingRepository = repositoryProvider.provide(),
        varselGateway = gatewayProvider.provide(),
        clock = clock
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun planleggFremtidigeVarsler(saksnummer: Fagsaknummer) {
        val meldeplikt = kelvinSakRepository.hentMeldeplikt(saksnummer)

        val meldeperioderMedMeldeplikt =
            kelvinSakService.hentMeldeperioder(FagsakReferanse(FagsystemNavn.KELVIN, saksnummer))
                .filter {
                    meldeplikt.contains(it.meldevindu.copy(fom = originalInnsendingstidspunkt(it.meldevindu.fom)))
                }

        if (meldeperioderMedMeldeplikt.size != meldeplikt.size) {
            log.warn("Meldevindu i meldeperioder samsvarer ikke med meldeplikt-perioder fra Kelvin. Saksnummer: ${saksnummer.asString}")
        }

        varselRepository.slettPlanlagteVarsler(saksnummer, TypeVarselOm.MELDEPLIKTPERIODE)
        val sendteVarsler = varselRepository.hentVarsler(saksnummer).filter { it.status == VarselStatus.SENDT }
        inaktiverVarslerForFjernetMeldeplikt(meldeperioderMedMeldeplikt, sendteVarsler)
        lagreFremtidigeVarsler(saksnummer, meldeperioderMedMeldeplikt, sendteVarsler)
    }

    private fun inaktiverVarslerForFjernetMeldeplikt(
        meldeperioder: List<Meldeperiode>,
        sendteVarsler: List<Varsel>
    ) {
        sendteVarsler
            .filterNot { varsel -> meldeperioder.map { it.meldeperioden }.contains(varsel.forPeriode) }
            .forEach { varsel ->
                inaktiverVarsel(varsel)
            }
    }

    private fun lagreFremtidigeVarsler(
        saksnummer: Fagsaknummer,
        meldeperioder: List<Meldeperiode>,
        sendteVarsler: List<Varsel>
    ) {
        val utfyllinger = hentFerdigeUtfyllinger(saksnummer)
        val fremtidigeMeldeperioder =
            meldeperioder.filter { meldeperiode ->
                val muligTidligereInnsendingstidspunkt = tidligsteInnsendingstidspunkt(meldeperiode.meldevindu.fom)
                val meldevindu = Periode(muligTidligereInnsendingstidspunkt, meldeperiode.meldevindu.tom)
                val erIMeldevinduet = meldevindu.contains(LocalDate.now(clock))
                val harSendtVarselForPerioden = sendteVarsler.map { it.forPeriode }.contains(meldeperiode.meldeperioden)
                val meldevinduIFremtiden = meldeperiode.meldevindu.fom.isAfter(LocalDate.now(clock))

                (erIMeldevinduet && !harSendtVarselForPerioden) || meldevinduIFremtiden
            }
        fremtidigeMeldeperioder
            .filterNot { harUtfylling(it.meldeperioden, utfyllinger) }
            .map { lagVarsel(saksnummer, it, TypeVarselOm.MELDEPLIKTPERIODE) }
            .forEach { varsel ->
                varselRepository.upsert(varsel)
            }
    }

    private fun harUtfylling(forPeriode: Periode, utfyllinger: List<Utfylling>): Boolean {
        return utfyllinger.any { it.periode.overlapper(forPeriode) }
    }

    private fun hentFerdigeUtfyllinger(saksnummer: Fagsaknummer): List<Utfylling> {
        return utfyllingRepository.hentUtfyllinger(saksnummer).filter { it.erAvsluttet }
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
                log.info("Sender ${it.size} varsler")
            }
            .forEach { varsel ->
                sendVarsel(varsel)
            }
    }

    private fun sendVarsel(varsel: Varsel) {
        if (harUtfylling(varsel.forPeriode, hentFerdigeUtfyllinger(varsel.saksnummer))) {
            log.info(
                "Avbryter sending og sletter planlagt varsel som allerede har en utfylling." +
                        "Saksnummer: ${varsel.saksnummer.asString}, varselId: ${varsel.varselId}, forPeriode: ${varsel.forPeriode}, sendingstidspunkt: ${varsel.sendingstidspunkt}"
            )
            varselRepository.slettVarsel(varsel.varselId)
            return
        }

        if (varsel.sendingstidspunkt.atZone(ZoneId.systemDefault()).toLocalDate().isEqual(LocalDate.now(clock))) {
            log.info("Sender varsel med varselId ${varsel.varselId}")
        } else {
            log.warn(
                "Sender varsel som skulle sendes en annen dag enn i dag. Tyder på forsinkelse i utsendingsjobb. " +
                        "Saksnummer: ${varsel.saksnummer.asString}, varselId: ${varsel.varselId}, forPeriode: ${varsel.forPeriode}, sendingstidspunkt: ${varsel.sendingstidspunkt}"
            )
        }

        varselRepository.upsert(
            varsel.copy(
                status = VarselStatus.SENDT,
                sistEndret = Instant.now(clock),
            )
        )

        // TODO hent gjeldende ident fra PDL
        val brukerId = kelvinSakRepository.hentIdenter(varsel.saksnummer).first()
        val varselTekster = when (varsel.typeVarselOm) {
            TypeVarselOm.MELDEPLIKTPERIODE -> TEKSTER_OPPGAVE_MELDEPLIKTPERIODE
            else -> TODO("ikke implenetert støtte for varseltype ${varsel.typeVarselOm}")
        }
        varselGateway.sendVarsel(
            brukerId = brukerId,
            varsel = varsel,
            varselTekster = varselTekster,
            lenke = requiredConfigForKey("aap.meldekort.lenke")
        )
    }

    fun inaktiverVarslerForUtfylling(utfylling: Utfylling) {
        if (utfylling.fagsak.system != FagsystemNavn.KELVIN) return

        val varsler = varselRepository.hentVarsler(utfylling.fagsak.nummer)
            .filter { varsel ->
                varsel.status == VarselStatus.SENDT &&
                        varsel.typeVarsel == TypeVarsel.OPPGAVE &&
                        utfylling.periode.overlapper(varsel.forPeriode)
            }

        if (varsler.isNotEmpty()) {
            varsler.forEach { inaktiverVarsel(it) }
        } else {
            log.info("Fant ikke varsel å inaktivere for utfylling. Referanse: ${utfylling.referanse.asUuid}, saksnummer: ${utfylling.fagsak.nummer.asString}, periode: ${utfylling.periode}")
        }
    }

    private fun inaktiverVarsel(varsel: Varsel) {
        log.info("Inaktiverer varsel med varsel id ${varsel.varselId}")
        varselRepository.upsert(
            varsel.copy(
                status = VarselStatus.INAKTIVERT,
                sistEndret = Instant.now(clock)
            )
        )
        varselGateway.inaktiverVarsel(varsel)
    }
}
