package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.delete
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import java.time.LocalDate

class MeldeperiodeDto(
    val meldeperiode: PeriodeDto,
    val fastsattDag: LocalDate,
    val innsendingsvindu: PeriodeDto,
)

class InfoDto(
    val nåværendeMeldeperiode: MeldeperiodeDto?,
    val forrigeMeldeperiode: MeldeperiodeDto?,
    val nesteMeldeperiode: MeldeperiodeDto?,
    /* Et eller annet om situasjonen til bruker. Call-to-action? Status på forrige/tidligere meldeperioder? */
)

enum class StegDto {
    INTRODUKSJON,
    SPØRSMÅL,
    UTFYLLING,
    BEKREFT,
    KVITTERING,
}

class FlytTilstandDto(
    val steg: StegDto,
    val skjema: SkjemaDto,
)

sealed interface FlytTilstandResponseDto {
    class Ok(
        val flytTilstand: FlytTilstandDto,
    ): FlytTilstandResponseDto

    class Feil: FlytTilstandResponseDto
}

class SkjemaDto(
    val harDuJobbet: Boolean?,
    val dager: List<SkjemadagDto>,
)

class SkjemadagDto(
    val dato: LocalDate,
    val timerArbeidet: Double?,
)

fun NormalOpenAPIRoute.kelvinApi() {
    var flytTilstand: FlytTilstandDto? = null

    route("/api/") {
        route("bruker-info").get<Unit, InfoDto> {
            respond(InfoDto(
                forrigeMeldeperiode = MeldeperiodeDto(
                    meldeperiode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14)),
                    fastsattDag = LocalDate.of(2025, 1, 1),
                    innsendingsvindu = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 9)),
                ),
                nåværendeMeldeperiode = MeldeperiodeDto(
                    meldeperiode = PeriodeDto(LocalDate.of(2025, 1, 1 + 14), LocalDate.of(2025, 1, 14 + 14)),
                    fastsattDag = LocalDate.of(2025, 1, 1 + 14),
                    innsendingsvindu = PeriodeDto(LocalDate.of(2025, 1, 1 + 14), LocalDate.of(2025, 1, 9 + 14)),
                ),
                nesteMeldeperiode = null,
            ))
        }

        route("utfylling") {
            route("tilstand") {
                get<Unit, FlytTilstandDto> {
                    respond(flytTilstand ?: FlytTilstandDto(StegDto.INTRODUKSJON, SkjemaDto(null, listOf())))
                }
                delete<Unit, Unit> {
                    flytTilstand = null
                    respondWithStatus(HttpStatusCode.OK)
                }
            }

            route("lagre-neste").post<Unit, FlytTilstandResponseDto, FlytTilstandDto> { params, body ->
                if (flytTilstand == null) {
                    respond(FlytTilstandResponseDto.Feil())
                } else {
                    flytTilstand = FlytTilstandDto(
                        steg = StegDto.entries.dropWhile { it != body.steg }.getOrElse(1) { StegDto.KVITTERING },
                        skjema = body.skjema,
                    ).also {
                        respond(FlytTilstandResponseDto.Ok(it))
                    }
                }
            }

            route("lagre").post<Unit, FlytTilstandResponseDto, FlytTilstandDto> { params, body ->
                if (flytTilstand == null) {
                    respond(FlytTilstandResponseDto.Feil())
                } else {
                    flytTilstand = body.also {
                        respond(FlytTilstandResponseDto.Ok(it))
                    }
                }
            }
        }
    }
}