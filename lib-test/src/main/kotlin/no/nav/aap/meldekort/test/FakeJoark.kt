package no.nav.aap.meldekort.test

import io.ktor.server.application.*
import org.slf4j.LoggerFactory

object FakeJoark: FakeServer {
    val log = LoggerFactory.getLogger(this.javaClass)!!

    override fun setProperties(port: Int) {
        System.setProperty("joark.url", "http://localhost:$port")
        System.setProperty("joark.scope", "joark-scope")
    }

    override val module: Application.() -> Unit = {}
}
