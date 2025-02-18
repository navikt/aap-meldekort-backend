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
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldeperioderApi(dataSource: DataSource) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(body: MeldeperiodeFlate.() -> T): T {
        return dataSource.transaction {
            MeldeperiodeFlate.konstruer(innloggetBruker(), it).body()
        }
    }

    route("meldeperiode") {
        route("kommende").get<Unit, KommendeMeldeperioderDto> {
            val response = medFlate {
                val meldeperioder = aktuelleMeldeperioder(innloggetBruker())

                KommendeMeldeperioderDto(
                    antallUbesvarteMeldeperioder = 0,
                    nesteMeldeperiode = null,
                )
            }
            respond(response)
        }

        route("historiske").get<Unit, List<HistoriskMeldeperiodeDto>> {
            val response = medFlate {
                val meldeperioder = historiskeMeldeperioder(innloggetBruker())
                listOf<HistoriskMeldeperiodeDto>()
            }
            respond(response)
        }

        route("detaljer").post<Unit, PeriodeDetaljerDto, PeriodeDto> { params, request ->
            val response = medFlate {
                val detaljer = periodedetaljer(innloggetBruker(), Periode(request.fom, request.tom))

                PeriodeDetaljerDto(
                    periode = PeriodeDto(LocalDate.now(), LocalDate.now()),
                    status = MeldeperiodeStatusDto.KELVIN,
                    bruttoBeløp = null,
                    innsendtDato = null,
                    kanEndres = false,
                    type = MeldekortTypeDto.KELVIN,
                    svar = SvarDto(
                        vilSvareRiktig = null,
                        harDuJobbet = null,
                        dager = emptyList(),
                        stemmerOpplysningene = null,
                    )
                )
            }
            respond(response)
        }
    }
}
