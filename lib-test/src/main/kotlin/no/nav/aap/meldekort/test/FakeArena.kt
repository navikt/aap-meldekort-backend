package no.nav.aap.meldekort.test

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.concurrent.ConcurrentHashMap

object FakeArena : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("meldekortservice.url", "http://localhost:$port")
        System.setProperty("meldekortservice.scope", "api://meldekortservice:aap:api-intern/.default")
        System.setProperty("meldekortkontroll.url", "http://localhost:$port")
        System.setProperty("meldekortkontroll.scope", "api://meldekortservice:aap:api-intern/.default")
    }

    private val meldekort = ConcurrentHashMap<String, List<ArenaMeldekort>>()
    private val historiskeMeldekort = ConcurrentHashMap<String, List<ArenaMeldekort>>()

    fun upsertMeldekort(fnr: String, meldekortListe: List<ArenaMeldekort>) {
        meldekort[fnr] = meldekortListe
    }

    fun upsertHistoriskeMeldekort(fnr: String, meldekortListe: List<ArenaMeldekort>) {
        historiskeMeldekort[fnr] = meldekortListe
    }

    fun clear() {
        meldekort.clear()
        historiskeMeldekort.clear()
    }

    private fun arenaResponse(meldekortListe: List<ArenaMeldekort>?) = mapOf(
        "personId" to 0,
        "etternavn" to "",
        "fornavn" to "",
        "maalformkode" to "",
        "meldeform" to "",
        "meldekortListe" to meldekortListe,
    )

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            val mapper = DefaultJsonMapper.objectMapper()
            register(io.ktor.http.ContentType.Application.Json, JacksonConverter(mapper, true))
        }

        routing {
            get("/v2/meldekort") {
                val fnr = call.request.headers["ident"]
                call.respond(arenaResponse(fnr?.let { meldekort[it] }))
            }

            get("/v2/historiskemeldekort") {
                val fnr = call.request.headers["ident"]
                call.respond(arenaResponse(fnr?.let { historiskeMeldekort[it] }))
            }

            get("/v2/meldegrupper") { call.respondText("{}") }
            get("/v2/meldekortdetaljer") { call.respondText("{}") }
            get("/v2/korrigertMeldekort") { call.respondText("{}") }
            post("/api/v1/kontroll") { call.respondText("{}") }
        }
    }
}
