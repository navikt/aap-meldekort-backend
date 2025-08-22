package no.nav.aap.meldekort

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.opplysningsplikt.TimerArbeidet
import no.nav.aap.opplysningsplikt.TimerArbeidetRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import java.time.LocalDate
import javax.sql.DataSource

data class DetaljertMeldekortReq(
    val personIdent: String,
    val saksNummer: Fagsaknummer,
)


fun NormalOpenAPIRoute.hentMeldekortData (
    dataSource: DataSource,
    repositoryProvider: RepositoryProvider
){
    route("detaljertMeldekort").post<Unit, List<TimerArbeidet>, DetaljertMeldekortReq> { _, body ->
        dataSource.transaction { connection ->
            val timerArbeiderRepo = repositoryProvider.provide<TimerArbeidetRepository>()

            val timerArbeidet = timerArbeiderRepo.hentTimerArbeidet(
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

            val meldeperioder =


            respondWithStatus(DetaljertMeldekortResponse.fraDomene(timerArbeiderRepo))
        }
    }
}