package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.kelvin.MeldekortstatusService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import java.time.Clock
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortStatus(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    clock: Clock,
) {
    route("meldekort-status").get<Unit, MeldekortstatusDto> {

        val response = dataSource.transaction { connection ->
            val meldekortStatusService = MeldekortstatusService(repositoryRegistry.provider(connection), clock)

            val ident = innloggetBruker().ident

            meldekortStatusService.hentMeldekortstatus(ident)
                ?.let { MeldekortstatusDto.fraDomene(it) }
        }

        if (response == null) {
            respondWithStatus(HttpStatusCode.NotFound)
        } else {
            respond(response)
        }
    }
}
