package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.arena.MeldekortRepositoryPostgres
import no.nav.aap.arena.MeldekortStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.TimerArbeidet
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.jvm.javaClass

data class DetaljertMeldekortReq(
    val personIdent: String,
    val saksNummer: Fagsaknummer,
)

data class DetaljertMeldekortResponse(
    val meldeperioder: List<MeldekortPeriode>,
)

data class MeldekortPeriode(
    val meldekortStatus: MeldekortStatus,
    val periode: Periode,
    val timerArbeidet: List<TimerArbeidet>,
)

fun NormalOpenAPIRoute.hentMeldekortData(
    dataSource: DataSource,
    repositoryProvider: RepositoryProvider
) {
    route("detaljertMeldekort").post<Unit, DetaljertMeldekortResponse, DetaljertMeldekortReq> { _, body ->
        val log = LoggerFactory.getLogger(javaClass)
        val response = dataSource.transaction { connection ->

            val timerArbeidetRepo = repositoryProvider.provide<TimerArbeidetRepository>()
            val meldekortRepo = repositoryProvider.provide<MeldekortRepositoryPostgres>()

            val timerArbeidet = timerArbeidetRepo.hentTimerArbeidet(
                ident = Ident(body.personIdent),
                sak = FagsakReferanse(
                    system = FagsystemNavn.KELVIN,
                    nummer = body.saksNummer
                ),
                periode = Periode(
                    fom = LocalDate.now().minusYears(3),
                    tom = LocalDate.now().plusYears(1)
                )
            )

            DetaljertMeldekortResponse(listOf(MeldekortPeriode(MeldekortStatus.INNSENDT, Periode(
                fom = LocalDate.now().minusYears(3),
                tom = LocalDate.now().plusYears(1)
            ), timerArbeidet)))
        }
        log.info("stuff")
        respond(response)
    }

}