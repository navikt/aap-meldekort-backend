package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.meldeperiode.MeldeperiodeFlateFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldeperioderApi(dataSource: DataSource, meldeperiodeFlateFactory: MeldeperiodeFlateFactory) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(body: MeldeperiodeFlate.() -> T): T {
        return dataSource.transaction { connection ->
            val meldeperiodeFlate = meldeperiodeFlateFactory.flateForBruker(innloggetBruker(), connection)
            meldeperiodeFlate.body()
        }
    }

    route("meldeperiode") {
        route("kommende").get<Unit, KommendeMeldeperioderDto> {
            val response = medFlate {
                val kommendeMeldeperioder = aktuelleMeldeperioder(innloggetBruker())
                KommendeMeldeperioderDto.fraDomene(kommendeMeldeperioder)
            }
            respond(response)
        }

        route("historiske").get<Unit, List<HistoriskMeldeperiodeDto>> {
            val response = medFlate {
                val historiskeMeldeperioder = historiskeMeldeperioder(innloggetBruker())
                historiskeMeldeperioder.map {
                    HistoriskMeldeperiodeDto.fraDomene(it)
                }
            }
            respond(response)
        }

        route("detaljer").post<Unit, PeriodeDetaljerDto, PeriodeDto> { _, request ->
            val response = medFlate {
                val detaljer = periodedetaljer(innloggetBruker(), Periode(request.fom, request.tom))
                PeriodeDetaljerDto(detaljer)
            }
            respond(response)
        }
    }
}
