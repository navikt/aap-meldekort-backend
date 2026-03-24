package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.kelvin.MeldekortStatusService
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
            val meldekortStatusService = MeldekortStatusService(repositoryRegistry.provider(connection), clock)

            val ident = innloggetBruker().ident
            val sak = meldekortStatusService.brukerHarSakIKelvin(ident) ?: return@transaction null

            MeldekortstatusDto(
                harInnsendteMeldekort = meldekortStatusService.harInnsendteMeldekort(
                    ident,
                    sak.referanse
                ),
                meldekortTilUtfylling = meldekortStatusService.hentMeldekortTilUtfylling(
                    ident,
                    sak.referanse
                ).map { meldekort ->
                    MeldekortTilUtfyllingDto.fraDomene(meldekort)
                },
                redirectUrl = "www.nav.no/aap/meldekort", // TODO hent fra config
            )
        }

        if (response == null) {
            respondWithStatus(HttpStatusCode.NotFound)
        } else {
            respond(response)
        }
    }
}
