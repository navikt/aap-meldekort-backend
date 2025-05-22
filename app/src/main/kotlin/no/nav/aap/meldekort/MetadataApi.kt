package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import java.time.Clock
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.metadataApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    clock: Clock,
){
    route("hent-metadata").get<Unit, MetadataDto> {
        val response = dataSource.transaction { connection ->
            val kelvinSakRepository = repositoryRegistry.provider(connection).provide<KelvinSakRepository>()
            val kelvinSak = kelvinSakRepository.hentSak(innloggetBruker().ident, LocalDate.now(clock))
            MetadataDto(brukerHarVedtakIKelvin = kelvinSak?.erLøpende(), brukerHarSakUnderBehandling = kelvinSak?.erUnderBehandling())
        }
        respond(response)
    }
}