package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.meldeperiode.MeldeperiodeFlateFactory
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldeperioderApi(
    dataSource: DataSource,
    meldeperiodeFlateFactory: MeldeperiodeFlateFactory,
    repositoryRegistry: RepositoryRegistry,
) {
    val log = LoggerFactory.getLogger(javaClass)

    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(body: MeldeperiodeFlate.() -> T): T {
        return dataSource.transaction { connection ->
            val (saksnummer, meldeperiodeFlate) = meldeperiodeFlateFactory.flateForBruker(
                innloggetBruker = innloggetBruker(),
                repositoryProvider = repositoryRegistry.provider(connection),
                gatewayProvider = GatewayProvider
            )

            MDC.putCloseable("saksnummer", saksnummer).use {
                meldeperiodeFlate.body()
            }
        }
    }

    route("meldeperiode") {
        route("kommende").get<Unit, KommendeMeldeperioderDto> {
            val response = medFlate {
                val kommendeMeldeperioder = aktuelleMeldeperioder(innloggetBruker())
                log.info("Henter kommende meldeperioder. Fant ${kommendeMeldeperioder.antallUbesvarteMeldeperioder} ubsvarte meldeperioder.")
                KommendeMeldeperioderDto.fraDomene(kommendeMeldeperioder)
            }
            respond(response)
        }

        route("historiske").get<Unit, List<HistoriskMeldeperiodeDto>> {
            val response = medFlate {
                val historiskeMeldeperioder = historiskeMeldeperioder(innloggetBruker())
                log.info("Hentet ${historiskeMeldeperioder.size} historiske meldeperioder.")
                historiskeMeldeperioder.map {
                    HistoriskMeldeperiodeDto.fraDomene(it)
                }
            }
            respond(response)
        }

        route("detaljer").post<Unit, PeriodeDetaljerDto, PeriodeDto> { _, request ->
            val response = medFlate {
                val detaljer = periodedetaljer(innloggetBruker(), Periode(request.fom, request.tom))
                log.info("Henter detaljer for peride ${detaljer.periode}")
                PeriodeDetaljerDto(detaljer)
            }
            respond(response)
        }
    }
}
