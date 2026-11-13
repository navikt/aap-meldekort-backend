package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaoppslagGateway
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import javax.sql.DataSource

enum class AnsvarligMeldekortløsningDto {
    AAP,
    FELLES,
    ;
}

data class AnsvarligeMeldekortløsningerDto(
    val kelvin: MeldekortløsningDto?,
    val arena: MeldekortløsningDto?,
)

data class MeldekortløsningDto(
    val saksnummer: String,
    val rettighetsperiode: PeriodeDto,
)

fun NormalOpenAPIRoute.ansvarligSystemApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    clock: Clock,
) {
    val log = LoggerFactory.getLogger(javaClass)
    route("v2") {
        route("ansvarlig-system").get<Unit, AnsvarligeMeldekortløsningerDto> {
            val ident = personBrukerIdent()
            val idag = LocalDate.now(clock)
            val kelvinSak = dataSource.transaction { connection ->
                val kelvinSakRepository = repositoryRegistry.provider(connection).provide<KelvinSakRepository>()
                kelvinSakRepository.hentSak(ident, idag)
            }

            val arenaSaker = GatewayProvider.provide<ArenaoppslagGateway>()
                .hentSaker(ident)
            if (kelvinSak != null) {
                log.info("Kelvin: ${kelvinSak.saksnummer.asString}, ${kelvinSak.rettighetsperiode.fom} - ${kelvinSak.rettighetsperiode.tom}. Arena: $arenaSaker")
            }
            val arenaSak = if (kelvinSak != null) {
                val førsteDagIKelvin = kelvinSak.rettighetsperiode.fom
                val ukenFørKelvin = Periode(
                    fom = førsteDagIKelvin.minusDays(7),
                    tom = førsteDagIKelvin.minusDays(1),
                )

                arenaSaker.finnSakSomSlutterIPeriode(ukenFørKelvin)
            } else {
                log.info("Fant ingen sak i Kelvin, sjekker om vi finner en sak i Arena")
                arenaSaker.finnSakForDagen(idag)
            }
            val response = AnsvarligeMeldekortløsningerDto(
                kelvin = kelvinSak?.let {
                    MeldekortløsningDto(
                        saksnummer = it.saksnummer.asString,
                        rettighetsperiode = PeriodeDto(it.rettighetsperiode),
                    )
                },
                arena = arenaSak?.let {
                    MeldekortløsningDto(
                        saksnummer = it.referanse.nummer.asString,
                        rettighetsperiode = PeriodeDto(it.rettighetsperiode),
                    )
                },
            )
            log.info("Svarer med følgende response: ${response}")
            respond(response)
        }
    }

    route("ansvarlig-system").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val sakerService = SakerService(
                aapGateway = GatewayProvider.provide(),
                kelvinSakRepository = repositoryRegistry.provider(connection).provide(),
            )

            val sak = sakerService.finnSak(personBrukerIdent(), LocalDate.now(clock))
            when (sak?.referanse?.system) {
                FagsystemNavn.ARENA -> AnsvarligMeldekortløsningDto.FELLES
                null, FagsystemNavn.KELVIN -> AnsvarligMeldekortløsningDto.AAP
            }
        }
        respond(response)
    }

    route("ansvarlig-system-felles").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val kelvinSakRepository = repositoryRegistry.provider(connection).provide<KelvinSakRepository>()
            val kelvinSak = kelvinSakRepository.hentSak(personBrukerIdent(), LocalDate.now(clock))
            if (kelvinSak == null)
                AnsvarligMeldekortløsningDto.FELLES
            else
                AnsvarligMeldekortløsningDto.AAP
        }
        respond(response)
    }
}