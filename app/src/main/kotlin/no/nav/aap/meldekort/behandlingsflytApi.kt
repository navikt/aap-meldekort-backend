package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinMottakService
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.meldekort.kontrakt.sak.SakStatus
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedPost
import java.util.*
import javax.sql.DataSource


fun NormalOpenAPIRoute.behandlingsflytApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    val authorizedAzps = listOfNotNull(configForKey("BEHANDLINGSFLYT_AZP")?.let(UUID::fromString))

    route("/api/behandlingsflyt/sak/meldeperioder").authorizedPost<Unit, Unit, MeldeperioderV0>(
        routeConfig = AuthorizationMachineToMachineConfig(authorizedAzps = authorizedAzps),
        auditLogConfig = null,
    ) { _, body ->
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val kelvinMottakService = KelvinMottakService(repositoryProvider)

            kelvinMottakService.behandleMottatteMeldeperioder(
                saksnummer = Fagsaknummer(body.saksnummer),
                identer = body.identer.map { Ident(it) },
                sakenGjelderFor = Periode(body.sakenGjelderFor.fom, body.sakenGjelderFor.tom),
                meldeperioder = body.meldeperioder.map { Periode(it.fom, it.tom) },
                opplysningsbehov = body.opplysningsbehov.map { Periode(it.fom, it.tom) },
                meldeplikt = body.meldeperioder.map { Periode(it.fom, it.tom) },
                status = when (body.sakStatus) {
                    SakStatus.UTREDES -> KelvinSakStatus.UTREDES
                    SakStatus.LØPENDE -> KelvinSakStatus.LØPENDE
                    SakStatus.AVSLUTTET -> KelvinSakStatus.AVSLUTTET
                    null -> null
                }
            )
        }
        respondWithStatus(HttpStatusCode.OK)
    }
}
