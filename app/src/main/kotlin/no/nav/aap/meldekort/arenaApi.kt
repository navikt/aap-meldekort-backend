package no.nav.aap.meldekort

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.arena.MeldekortId
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.lookup.gateway.GatewayProvider

fun NormalOpenAPIRoute.arenaApi() {
    if (configForKey("nais.cluster.name") in listOf("dev-gcp", "local")) {
        val arenaGateway = GatewayProvider.provide<ArenaGateway>()
        route("/debug/arena-proxy") {
            data class MeldekortIdParam(
                @JsonValue @PathParam("meldekortId") val meldekortId: Long,
            )
            route("meldegrupper").get<Unit, Any> {
                respond(arenaGateway.meldegrupper(innloggetBruker()))
            }
            route("meldekort").get<Unit, Any> {
                respond(arenaGateway.person(innloggetBruker()) ?: "null")
            }
            route("historiskemeldekort").get<Unit, Any> {
                respond(arenaGateway.historiskeMeldekort(innloggetBruker(), antallMeldeperioder = 5))
            }
            route("meldekortdetaljer/{meldekortId}").get<MeldekortIdParam, Any> { params ->
                respond(arenaGateway.meldekortdetaljer(innloggetBruker(), MeldekortId(params.meldekortId)))
            }
            route("korrigerte-meldekort/{meldekortId}").get<MeldekortIdParam, Any> { params ->
                respond(arenaGateway.korrigertMeldekort(innloggetBruker(), MeldekortId(params.meldekortId)))
            }
        }
    }
}

