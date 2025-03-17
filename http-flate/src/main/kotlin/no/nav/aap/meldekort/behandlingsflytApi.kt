package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0


fun NormalOpenAPIRoute.behandlingsflytApi() {
    route("/api/behandlingsflyt/sak/meldeperioder").post<Unit, Unit, MeldeperioderV0>  { _, body ->
        respondWithStatus(HttpStatusCode.OK)
    }
}
