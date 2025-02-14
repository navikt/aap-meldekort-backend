package no.nav.aap.meldekort.test

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.LocalDate

object FakeAapApi : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("aap.api.intern.url", "http://localhost:$port")
        System.setProperty("aap.api.intern.scope", "api://local:aap:api-intern/.default")
    }

    override val module: Application.() -> Unit = {
        val idag = LocalDate.now()

        val responser = mapOf(
            "1".repeat(11) to """
                        [
                          {
                            "sakId": "1015",
                            "vedtakStatusKode": "REGIS",
                            "periode": {
                              "fraOgMedDato": "${idag.minusDays(100)}",
                              "tilOgMedDato": "${idag.plusDays(20)}"
                            },
                            "kilde": "Kelvin"
                          }
                        ]
                """,
            "2".repeat(11) to """
                [
                  {
                    "sakId": "13702335",
                    "vedtakStatusKode": "IVERK",
                    "periode": {
                      "fraOgMedDato": "${idag.minusDays(100)}",
                      "tilOgMedDato": "${idag.plusDays(20)}"
                    },
                    "kilde": "ARENA"
                  }
                ]
                """,
            "3".repeat(11) to "[]"
        )
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            post("/sakerByFnr") {
                val fnr = call.receive<JsonNode>().at("/personidentifikatorer/0").asText()
                println("fnr $fnr")
                call.respond(responser.getOrDefault(fnr, emptyList<Any>()))
            }
        }
    }
}