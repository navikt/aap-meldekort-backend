package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.SakerService
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import java.util.*

class JournalføringJobbUtfører(
    private val journalføringService: JournalføringService,
    private val utfyllingRepository: UtfyllingRepository,
    private val sakerService: SakerService,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val ident = Ident(input.parameter("ident"))
        val utfyllingReferanse = UtfyllingReferanse(UUID.fromString(input.parameter("utfylling")))
        val fagsakReferanse = FagsakReferanse(
            FagsystemNavn.valueOf(input.parameter("fagsak_system")),
            Fagsaknummer(input.parameter("fagsak_nummer"))
        )

        val sak = sakerService.finnSak(ident, fagsakReferanse)!!

        journalføringService.journalfør(
            ident = ident,
            utfylling = utfyllingRepository.lastAvsluttetUtfylling(ident, utfyllingReferanse)!!,
            sak = sak,
        )
    }

    companion object : Jobb {
        fun jobbInput(
            ident: Ident,
            utfylling: UtfyllingReferanse,
            fagsak: FagsakReferanse,
        ): JobbInput {
            return JobbInput(JournalføringJobbUtfører).apply {
                medParameter("ident", ident.asString)
                medParameter("utfylling", utfylling.asUuid.toString())
                medParameter("fagsak_system", fagsak.system.toString())
                medParameter("fagsak_nummer", fagsak.nummer.asString)
            }
        }

        override fun navn(): String {
            return "JournalføringJobbUtfører"
        }

        override fun type(): String {
            return "meldekort.journalføring"
        }

        override fun beskrivelse(): String {
            return "Journalfør ferdig utfylt meldekort"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            return JournalføringJobbUtfører(
                journalføringService = JournalføringService.konstruer(connection),
                utfyllingRepository = repositoryProvider.provide(),
                sakerService = SakerService.konstruer(connection),
            )
        }
    }

    object LogInfoProvider : JobbLogInfoProvider {
        override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon {
            return LogInformasjon(
                listOf("utfylling", "fagsak_system", "fagsak_nummer")
                    .mapNotNull { name -> jobbInput.optionalParameter(name)?.let { value -> name to value } }
                    .toMap()
            )
        }
    }
}
