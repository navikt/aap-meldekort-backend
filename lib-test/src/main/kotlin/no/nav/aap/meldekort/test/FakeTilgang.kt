package no.nav.aap.meldekort.test

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangResponse

object FakeTilgang : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("INTEGRASJON_TILGANG_URL", "http://localhost:$port")
        System.setProperty("INTEGRASJON_TILGANG_SCOPE", "api://local:aap:tilgang/.default")
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        routing {
            post("/tilgang/sak") {
                call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/behandling") {
                call.receive<BehandlingTilgangRequest>()
                call.respond(
                    TilgangResponse(
                        true,
                        tilgangIKontekst = mapOf(Operasjon.SAKSBEHANDLE to true)
                    )
                )
            }
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }
}