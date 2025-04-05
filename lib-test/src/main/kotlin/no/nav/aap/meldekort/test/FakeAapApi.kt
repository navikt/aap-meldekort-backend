package no.nav.aap.meldekort.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.FagsystemNavn.ARENA
import no.nav.aap.sak.FagsystemNavn.KELVIN
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

object FakeAapApi : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("aap.api.intern.url", "http://localhost:$port")
        System.setProperty("aap.api.intern.scope", "api://local:aap:api-intern/.default")
    }

    private val saker = ConcurrentHashMap<String, List<FakeSak>>()

    class FakeSak(
        val referanse: FagsakReferanse,
        val rettighetsperiode: Periode,
    )

    fun upsert(ident: Ident, sak: FakeSak) {
        saker.compute(ident.asString) { _, oldValue ->
            oldValue.orEmpty()
                .filter { it.referanse != sak.referanse }
                .let { it + listOf(sak) }
        }
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            val mapper = DefaultJsonMapper.objectMapper()
            mapper.registerKotlinModule()
            val converter = JacksonConverter(mapper, true)
            register(ContentType.Application.Json, converter)
        }
        routing {
            post("/sakerByFnr") {
                val fnr = call.receive<JsonNode>().at("/personidentifikatorer/0").asText()
                call.respond(saker.getOrDefault(fnr, emptyList()).map {
                    mapOf(
                        "sakId" to it.referanse.nummer.asString,
                        "statusKode" to "IVERK",
                        "periode" to mapOf(
                            "fraOgMedDato" to it.rettighetsperiode.fom,
                            "tilOgMedDato" to it.rettighetsperiode.tom,
                        ),
                        "kilde" to when (it.referanse.system) {
                            ARENA -> "ARENA"
                            KELVIN -> "KELVIN"
                        }
                    )
                }.also {
                    log.info("sakerByFnr returnerer ${it.size} saker")
                })
            }

            post("/perioder/meldekort") {
                class Request(
                    val fraOgMedDato: LocalDate,
                    val personidentifikator: String,
                    val tilOgMedDato: LocalDate,
                ) {
                    val periode = Periode(fraOgMedDato, tilOgMedDato)
                }

                val request = call.receive<Request>()
                val perioder = saker[request.personidentifikator]
                    .orEmpty()
                    .filter { it.rettighetsperiode.overlapper(request.periode) }
                    .flatMap {
                        Periode(it.rettighetsperiode.fom, it.rettighetsperiode.tom).slidingWindow(
                            size = 14,
                            step = 14,
                            partialWindows = true
                        )
                    }
                call.respond(perioder)
            }
        }
    }
}