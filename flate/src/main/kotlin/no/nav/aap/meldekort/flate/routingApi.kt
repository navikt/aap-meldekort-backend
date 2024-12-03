package no.nav.aap.meldekort.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import javax.sql.DataSource

fun NormalOpenAPIRoute.routingApi(dataSource: DataSource) {
    route("/api/routing") {
        get<Unit, RoutingRespons> { req ->
            // Ruter innlogget bruker i henhold til gjeldende vedtak, enten Arena eller Kelvin
            respond(RoutingRespons(system = SaksbehandlingSystem.ARENA))
        }
    }
}
