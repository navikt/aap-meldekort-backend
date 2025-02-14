package no.nav.aap.meldekort.test

import io.ktor.server.application.*

object FakeArena: FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("meldekortservice.url", "http://localhost:$port")
        System.setProperty("meldekortservice.scope", "api://meldekortservice:aap:api-intern/.default")
        System.setProperty("meldekortkontroll.url", "http://localhost:$port")
        System.setProperty("meldekortkontroll.scope", "api://meldekortservice:aap:api-intern/.default")
    }

    override val module: Application.() -> Unit = {}
}
