package no.nav.aap.meldekort.test

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.Ident
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.concurrent.ConcurrentHashMap

object FakeArenaoppslag : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("integrasjon.arenaoppslag.url", "http://localhost:$port")
        System.setProperty("integrasjon.arenaoppslag.scope", "api://local.aap.arenaoppslag/.default")
    }

    private val vedtak = ConcurrentHashMap<String, List<ArenaVedtak>>()
    private val responser = ConcurrentHashMap<String, Pair<HttpStatusCode, String>>()
    val forespørsler = ConcurrentHashMap<String, VedtakForPersonRequest>()

    fun settVedtak(ident: Ident, nyeVedtak: List<ArenaVedtak>) {
        vedtak[ident.asString] = nyeVedtak
    }

    fun leggTilVedtak(ident: Ident, nyttVedtak: ArenaVedtak) {
        vedtak.compute(ident.asString) { _, eksisterende -> eksisterende.orEmpty() + nyttVedtak }
    }

    fun settRespons(ident: Ident, status: HttpStatusCode, body: String) {
        responser[ident.asString] = status to body
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(DefaultJsonMapper.objectMapper(), true))
        }
        routing {
            post("/api/v1/person/vedtak") {
                if (call.request.headers[HttpHeaders.Authorization]?.startsWith("Bearer ") != true) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val request = call.receive<VedtakForPersonRequest>()
                forespørsler[request.personidentifikator] = request
                val respons = responser[request.personidentifikator]
                if (respons != null) {
                    call.respondText(respons.second, ContentType.Application.Json, respons.first)
                } else {
                    val personVedtak = vedtak[request.personidentifikator]
                    if (personVedtak == null) {
                        call.respondText("Fant ikke personen i Arena", status = HttpStatusCode.NotFound)
                    } else {
                        call.respond(personVedtak)
                    }
                }
            }
        }
    }
}
