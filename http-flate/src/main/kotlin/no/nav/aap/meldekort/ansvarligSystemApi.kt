package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.AnsvarligFlate
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.sak.FagsystemNavn
import javax.sql.DataSource

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

fun NormalOpenAPIRoute.ansvarligSystemApi(dataSource: DataSource) {
    route("ansvarlig-system").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val ansvarligFlate = AnsvarligFlate.konstruer(connection)
            AnsvarligMeldekortløsningDto.fromFagsystem(
                ansvarligFlate.routingForBrukerHosOss(innloggetBruker())
            )
        }
        respond(response)
    }

    route("ansvarlig-system-felles").get<Unit, AnsvarligMeldekortløsningDto> {
        val response = dataSource.transaction { connection ->
            val ansvarligFlate = AnsvarligFlate.konstruer(connection)
            AnsvarligMeldekortløsningDto.fromFagsystem(
                ansvarligFlate.routingForBrukerHosFelles(innloggetBruker())
            )
        }
        respond(response)
    }

    if (configForKey("nais.cluster.name") in listOf("dev-gcp", "local")) {
        dataSource.transaction { connection ->
            val ansvarligFlate = AnsvarligFlate.konstruer(connection)
            route("/debug/saker").get<Unit, Any> {
                respond(ansvarligFlate.debugSaker(innloggetBruker()))
            }
        }
    }
}