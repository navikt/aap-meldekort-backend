package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.AapGateway
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.Clock
import java.time.LocalDate
import javax.sql.DataSource

enum class AnsvarligMeldekortløsningDto {
    AAP,
    FELLES,
    ;
}

data class AnsvarligeMeldekortløsningerDto(
    val løsninger: List<MeldekortløsningDto>,
)

data class MeldekortløsningDto(
    val system: AnsvarligMeldekortløsningDto,
    val saksnummer: String?,
    val rettighetsperiode: PeriodeDto?,
)

fun NormalOpenAPIRoute.ansvarligSystemApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    clock: Clock,
) {
    route("v2") {
        route("ansvarlig-system").get<Unit, AnsvarligeMeldekortløsningerDto> {
            val response = dataSource.transaction { connection ->
                val kelvinSakRepository = repositoryRegistry.provider(connection).provide<KelvinSakRepository>()
                val ident = personBrukerIdent()
                val idag = LocalDate.now(clock)
                val kelvinSak = kelvinSakRepository.hentSak(ident, idag)
                val arenaDato = kelvinSak?.rettighetsperiode?.fom?.minusDays(1) ?: idag
                val arenaSak = GatewayProvider.provide<AapGateway>()
                    .hentSakerFraArena(ident)
                    .finnSakForDagen(arenaDato)
                AnsvarligeMeldekortløsningerDto(
                    løsninger = listOfNotNull(
                        if (kelvinSak != null || arenaSak == null) {
                            MeldekortløsningDto(
                                system = AnsvarligMeldekortløsningDto.AAP,
                                saksnummer = kelvinSak?.saksnummer?.asString,
                                rettighetsperiode = PeriodeDto.ellerNull(kelvinSak?.rettighetsperiode),
                            )
                        } else null,
                        arenaSak?.let {
                            MeldekortløsningDto(
                                system = AnsvarligMeldekortløsningDto.FELLES,
                                saksnummer = it.referanse.nummer.asString,
                                rettighetsperiode = PeriodeDto(it.rettighetsperiode),
                            )
                        },
                    ),
                )
            }
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