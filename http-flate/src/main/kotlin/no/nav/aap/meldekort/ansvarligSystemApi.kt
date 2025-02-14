package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.AnsvarligFlate
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.sak.Fagsystem
import no.nav.aap.sak.SakerGateway

enum class AnsvarligMeldekortløsningDto {
    AAP,
    FELLES,
    ;

    companion object {
        fun fromFagsystem(fagsystem: Fagsystem): AnsvarligMeldekortløsningDto {
            return when (fagsystem) {
                Fagsystem.ARENA -> FELLES
                Fagsystem.KELVIN -> AAP
            }
        }
    }
}

fun NormalOpenAPIRoute.ansvarligSystemApi() {
    val ansvarligFlate = AnsvarligFlate(
        sakerGateway = GatewayProvider.provide(SakerGateway::class)
    )
    route("/api/ansvarlig-system").get<Unit, AnsvarligMeldekortløsningDto> {
        /* Basert på: */
        /* Finnes sak(er) i kelvin, og hvilke rettighetsperioder? */
        /* Er det ATTF-meldegruppe i Arena? */
        respond(
            AnsvarligMeldekortløsningDto.fromFagsystem(
                ansvarligFlate.aktivtFagsystem(innloggetBruker())
            )
        )
    }

    if (configForKey("nais.cluster.name") in listOf("dev-gcp", "local")) {
        route("/api/debug/saker").get<Unit, Any> {
            respond(ansvarligFlate.debugSaker(innloggetBruker()))
        }
    }
}