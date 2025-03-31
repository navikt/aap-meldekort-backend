package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import java.time.LocalDate
import javax.sql.DataSource

enum class AnsvarligMeldekortløsningDto {
    AAP,
    FELLES,
    ;
}

fun NormalOpenAPIRoute.ansvarligSystemApi(dataSource: DataSource) {
    route("ansvarlig-system").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val sakerService = SakerService(
                aapGateway = GatewayProvider.provide(),
                kelvinSakRepository = RepositoryProvider(connection).provide(),
            )

            val sak = sakerService.finnSak(innloggetBruker().ident, LocalDate.now())
            when (sak?.referanse?.system) {
                FagsystemNavn.ARENA -> AnsvarligMeldekortløsningDto.FELLES
                null, FagsystemNavn.KELVIN -> AnsvarligMeldekortløsningDto.AAP
            }
        }
        respond(response)
    }

    route("ansvarlig-system-felles").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val kelvinSakRepository = RepositoryProvider(connection).provide<KelvinSakRepository>()
            val kelvinSak = kelvinSakRepository.hentSak(innloggetBruker().ident, LocalDate.now())
            if (kelvinSak == null)
                AnsvarligMeldekortløsningDto.FELLES
            else
                AnsvarligMeldekortløsningDto.AAP
        }
        respond(response)
    }
}