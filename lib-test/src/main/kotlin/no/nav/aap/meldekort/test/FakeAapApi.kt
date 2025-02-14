package no.nav.aap.meldekort.test

import io.ktor.server.application.*

object FakeAapApi:  FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("aap.api.intern.url", "http://localhost:$port")
        System.setProperty("aap.api.intern.scope", "api://local:aap:api-intern/.default")
    }

    override val module: Application.() -> Unit =  {
    }
}