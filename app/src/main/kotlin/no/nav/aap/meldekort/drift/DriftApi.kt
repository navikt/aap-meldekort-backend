package no.nav.aap.meldekort.drift

import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tags
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.varsel.VarselRepository
import javax.sql.DataSource

internal data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)

private enum class Tags(override val description: String) : APITag {
    DriftAPI("Driftsendepunkter for Paw Patrol"),
}

fun NormalOpenAPIRoute.driftApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/drift/sak/{saksnummer}/meldekort") {
        authorizedGet<SaksnummerParameter, Any>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFTE,
            ),
            modules = arrayOf(tags(Tags.DriftAPI))
        ) { params ->
            val saksnummer = params.saksnummer

            val dto = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)

                val utfyllinger =
                    repositoryProvider.provide<UtfyllingRepository>()
                        .hentUtfyllinger(Fagsaknummer(saksnummer))
                        .map { UtfyllingDriftsinfo.fra(it) }

                val varsler =
                    repositoryProvider.provide<VarselRepository>()
                        .hentVarsler(Fagsaknummer(saksnummer))
                        .map { VarselDriftsinfo.fra(it) }

                MeldekortDriftsinfoDto(utfyllinger, varsler)
            }

            respond(dto)
        }
    }

}
