package no.nav.aap.meldekort.test

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object FakeDokarkiv: FakeServer {
    val log = LoggerFactory.getLogger(this.javaClass)!!

    override fun setProperties(port: Int) {
        System.setProperty("dokarkiv.url", "http://localhost:$port")
        System.setProperty("dokarkiv.scope", "dokarkiv-scope")
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            val journalpostId = AtomicInteger(467010363)
            post("/rest/journalpostapi/v1/journalpost") {
                call.respondText("""
                    {
                      "dokumenter": [
                        {
                          "dokumentInfoId": "123"
                        }
                      ],
                      "journalpostId": "${journalpostId.getAndIncrement()}",
                      "journalpostferdigstilt": true
                    }
                """)
            }
        }
    }
}
