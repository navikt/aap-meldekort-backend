package no.nav.aap.meldekort.flate.arena

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortInfoApi(dataSource: DataSource) {
    route("/api/arena/meldekort") {
        route("/info") {
            get<Unit, MeldekortTilgjengeligRespons> { req ->
                // Ruter innlogget bruker i henhold til gjeldende vedtak, enten Arena eller Kelvin
                respond(MeldekortTilgjengeligRespons())
            }
        }
    }
}
