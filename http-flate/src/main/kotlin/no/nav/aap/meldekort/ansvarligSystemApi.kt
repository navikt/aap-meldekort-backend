package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route

enum class AnsvarligSystem {
    KELVIN,
    FELLES_MELDEKORTLØSNING,
}

fun NormalOpenAPIRoute.ansvarligSystemApi() {
    route("/api/ansvarlig-system").get<Unit, AnsvarligSystem> {
        /* Basert på: */
        /* Finnes sak(er) i kelvin, og hvilke rettighetsperioder? */
        /* Er det ATTF-meldegruppe i Arena? */
        respond(AnsvarligSystem.KELVIN)
    }
}