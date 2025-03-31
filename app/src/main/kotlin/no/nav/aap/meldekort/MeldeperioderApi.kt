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

fun NormalOpenAPIRoute.meldeperioderApi(dataSource: DataSource, dagensDato: LocalDate? = null) {
    fun <T> OpenAPIPipelineResponseContext<*>.medFlate(body: MeldeperiodeFlate.() -> T): T {
        return dataSource.transaction {
            MeldeperiodeFlate.konstruer(innloggetBruker().ident, it).body()
        }
    }

    route("meldeperiode") {
        route("kommende").get<Unit, KommendeMeldeperioderDto> {
            val response = medFlate {
                val ventendeOgNeste = aktuelleMeldeperioder(innloggetBruker(), dagensDato = dagensDato)

                val manglerOpplysninger = ventendeOgNeste.ventende
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        PeriodeDto(
                            fom = it.first().meldeperioden.fom,
                            tom = it.last().meldeperioden.tom,
                        )
                    }

                KommendeMeldeperioderDto(
                    antallUbesvarteMeldeperioder = ventendeOgNeste.ventende.size,
                    manglerOpplysninger = manglerOpplysninger,
                    nesteMeldeperiode = ventendeOgNeste.neste?.let { MeldeperiodeDto(it) },
                )
            }
            respond(response)
        }

        route("historiske").get<Unit, List<HistoriskMeldeperiodeDto>> {
            val response = medFlate {
                val meldeperioder = historiskeMeldeperioder(innloggetBruker())
                meldeperioder.map {
                    HistoriskMeldeperiodeDto(
                        meldeperiode = it,
                        antallTimerArbeidetIPerioden = totaltAntallTimerIPerioden(innloggetBruker(), it.meldeperioden)
                            ?: 0.0
                    )
                }
            }
            respond(response)
        }

        route("detaljer").post<Unit, PeriodeDetaljerDto, PeriodeDto> { params, request ->
            val response = medFlate {
                val detaljer = periodedetaljer(innloggetBruker(), Periode(request.fom, request.tom))
                PeriodeDetaljerDto(detaljer)
            }
            respond(response)
        }
    }
}
