package no.nav.aap.meldekort.test

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object FakeArena: FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("meldekortservice.url", "http://localhost:$port")
        System.setProperty("meldekortservice.scope", "api://meldekortservice:aap:api-intern/.default")
        System.setProperty("meldekortkontroll.url", "http://localhost:$port")
        System.setProperty("meldekortkontroll.scope", "api://meldekortservice:aap:api-intern/.default")
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }

        routing {
            get("/v2/meldegrupper") {
                call.respondText("{}")
            }

            get("/v2/meldekort") {
                call.respondText("{}")
            }

            get("/v2/historiskemeldekort") {
                call.respondText("{}")
            }

            get("/v2/meldekortdetaljer") {
                call.respondText("{}")
            }

            get("/v2/korrigertMeldekort") {
                call.respondText("{}")
            }

            post("/api/v1/kontroll") {
                call.respondText("{}")
            }
        }
    }
}
