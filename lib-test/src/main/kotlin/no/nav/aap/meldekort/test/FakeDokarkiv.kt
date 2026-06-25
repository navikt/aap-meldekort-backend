package no.nav.aap.meldekort.test

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.atomic.AtomicInteger

object FakeDokarkiv: FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("DOKARKIV_URL", "http://localhost:$port")
        System.setProperty("DOKARKIV_SCOPE", "dokarkiv-scope")
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            val journalpostId = AtomicInteger(467010363)
            post("/rest/journalpostapi/v1/journalpost") {
                println("JOURNALPOST mottatt: ${call.receiveText()}")
                call.respondText(
                    """
                        {
                        "journalpostId": ${journalpostId.getAndIncrement()},
                        "journalpostferdigstilt": true,
                        "dokumenter": [{
                            "dokumentInfoId": 4
                        }]
                        }
                    """.trimIndent()
                )
            }
        }
    }
}