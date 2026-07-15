package no.nav.aap.meldekort.drift

import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tags
import no.nav.aap.kelvin.KelvinMeldeperiodeFlate
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.varsel.VarselRepository
import java.time.Clock
import javax.sql.DataSource

internal data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)

private enum class Tags(override val description: String) : APITag {
    DriftAPI("Driftsendepunkter for Paw Patrol"),
}

fun NormalOpenAPIRoute.driftApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry, clock: Clock) {
    route("/api/drift/sak/{saksnummer}/meldekort") {
        authorizedGet<SaksnummerParameter, Any>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFTE,
            ),
            modules = arrayOf(tags(Tags.DriftAPI))
        ) { params ->
            val saksnummer = Fagsaknummer(params.saksnummer)

            val dto = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<KelvinSakRepository>()

                val sak = requireNotNull(sakRepository.hentSak(saksnummer)) {
                    "Sak med saksnummer ${saksnummer.asString} finnes ikke"
                }
                val identer = sakRepository.hentIdenter(saksnummer)

                val meldeperiodeFlate = KelvinMeldeperiodeFlate(repositoryProvider, clock)

                val aktuelleMeldeperioder = identer
                    .map { meldeperiodeFlate.aktuelleMeldeperioder(it) }
                    .map(AktuelleMeldeperioderDriftsinfo::fra)

                val historiskeMeldeperioder = identer
                    .flatMap { meldeperiodeFlate.historiskeMeldeperioder(it) }
                    .map(HistoriskeMeldeperioderDriftsinfo::fra)

                val utfyllinger =
                    repositoryProvider.provide<UtfyllingRepository>()
                        .hentUtfyllinger(saksnummer)
                        .map { UtfyllingDriftsinfo.fra(it) }

                val varsler =
                    repositoryProvider.provide<VarselRepository>()
                        .hentVarsler(saksnummer)
                        .map { VarselDriftsinfo.fra(it) }

                MeldekortDriftsinfoDto(sak, aktuelleMeldeperioder, historiskeMeldeperioder, utfyllinger, varsler)
            }

            respond(dto)
        }
    }

}
