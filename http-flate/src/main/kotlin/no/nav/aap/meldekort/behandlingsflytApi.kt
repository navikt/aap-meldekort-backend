package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedPost
import java.util.*


fun NormalOpenAPIRoute.behandlingsflytApi() {
    val authorizedAzps = listOfNotNull(configForKey("BEHANDLINGSFLYT_AZP")?.let(UUID::fromString))

    route("/api/behandlingsflyt/sak/meldeperioder").authorizedPost<Unit, Unit, MeldeperioderV0>(
        routeConfig = AuthorizationMachineToMachineConfig( authorizedAzps = authorizedAzps,  ),
        auditLogConfig = null,
    ) { _, body ->
        respondWithStatus(HttpStatusCode.OK)
    }
}
