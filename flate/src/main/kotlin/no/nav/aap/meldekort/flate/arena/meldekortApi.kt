package no.nav.aap.meldekort.flate.arena

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import javax.sql.DataSource

private var meldekortStore = Meldekort(
    null, null, List(14) { null }, null
)

private var stegStore = StegNavn.BEKREFT_SVARER_ÆRLIG

fun NormalOpenAPIRoute.meldekortInfoApi(dataSource: DataSource) {
    route("/api/arena/meldekort") {
        get<Unit, MeldekortResponse> { _ ->
            respond(
                MeldekortResponse(
                    stegStore,
                    meldekortStore
                )
            )
        }

        route("/neste-steg").post<Unit, MeldekortResponse, MeldekortRequest> { _, meldekortRequest ->
            meldekortStore = meldekortRequest.meldekort()
            val response = meldekortRequest.meldekortResponse()
            response?.steg?.let { stegStore = it }

            response?.let {
                respond(it)
            } ?: respondWithStatus(HttpStatusCode.BadRequest)
        }

        route("/lagre-tilstand").post<Unit, MeldekortResponse, MeldekortRequest> { _, meldekortRequest ->
            val meldekort = meldekortRequest.meldekort()
            meldekortStore = meldekort
            stegStore = meldekortRequest.nåværendeSteg
            respond(
                MeldekortResponse(meldekortRequest.nåværendeSteg, meldekort)
            )
        }
    }
}

data class MeldekortRequest(
    val nåværendeSteg: StegNavn,
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
) {
    fun meldekort(): Meldekort {
        return Meldekort(
            svarerDuSant = svarerDuSant,
            harDuJobbet = harDuJobbet,
            timerArbeidet = timerArbeidet,
            stemmerOpplysningene = stemmerOpplysningene
        )
    }

    fun meldekortResponse(): MeldekortResponse? {
        return nåværendeSteg.steg.nesteSteg(meldekort())?.let {
            MeldekortResponse(it.stegNavn, meldekort())
        }
    }
}

data class Meldekort(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<Int?>,
    val stemmerOpplysningene: Boolean?
)

data class MeldekortResponse(
    val steg: StegNavn,
    val meldekort: Meldekort,
)


enum class StegNavn(val steg: Steg) {
    BEKREFT_SVARER_ÆRLIG(BekreftSvarerÆrlig),
    JOBBET_I_MELDEPERIODEN(JobbetIMeldeperioden),
    TIMER_ARBEIDET(TimerArbeidet),
    KVITTERING(Kvittering)
}

interface Steg {
    val stegNavn: StegNavn
    fun nesteSteg(meldekort: Meldekort): Steg?
}


object BekreftSvarerÆrlig: Steg {
    override val stegNavn
        get() = StegNavn.BEKREFT_SVARER_ÆRLIG

    override fun nesteSteg(meldekort: Meldekort): Steg? {
        return when (meldekort.svarerDuSant) {
            true -> JobbetIMeldeperioden
            false -> Kvittering
            null -> null
        }
    }
}

object JobbetIMeldeperioden: Steg {
    override val stegNavn
        get() = StegNavn.JOBBET_I_MELDEPERIODEN

    override fun nesteSteg(meldekort: Meldekort): Steg? {
        return when (meldekort.harDuJobbet) {
            true -> TimerArbeidet
            false -> Kvittering
            null -> null
        }
    }
}

object TimerArbeidet: Steg {
    override val stegNavn
        get() = StegNavn.TIMER_ARBEIDET

    override fun nesteSteg(meldekort: Meldekort): Steg? {
        return when (meldekort.stemmerOpplysningene) {
            true -> Kvittering
            false -> null
            null -> null
        }
    }
}

object Kvittering: Steg {
    override val stegNavn
        get() = StegNavn.KVITTERING

    override fun nesteSteg(meldekort: Meldekort): Steg? {
        return null
    }
}