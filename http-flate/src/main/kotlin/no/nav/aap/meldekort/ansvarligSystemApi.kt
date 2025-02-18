package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.AnsvarligFlate
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.sak.FagsystemNavn

enum class AnsvarligMeldekortløsningDto {
    AAP,
    FELLES,
    ;

    companion object {
        fun fromFagsystem(fagsystemNavn: FagsystemNavn): AnsvarligMeldekortløsningDto {
            return when (fagsystemNavn) {
                FagsystemNavn.ARENA ->
                    if (configForKey("nais.cluster.name") in listOf(null, "local", "dev-gcp")) AAP else FELLES
                FagsystemNavn.KELVIN ->
                    AAP
            }
        }
    }
}

fun NormalOpenAPIRoute.ansvarligSystemApi() {
    val ansvarligFlate = AnsvarligFlate.konstruer()
    route("ansvarlig-system").get<Unit, AnsvarligMeldekortløsningDto> {
        respond(
            AnsvarligMeldekortløsningDto.fromFagsystem(
                ansvarligFlate.routingForBruker(innloggetBruker())
            )
        )
    }

    if (configForKey("nais.cluster.name") in listOf("dev-gcp", "local")) {
        route("/debug/saker").get<Unit, Any> {
            respond(ansvarligFlate.debugSaker(innloggetBruker()))
        }
    }
}