package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.gateway.GatewayProvider
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

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        journalføringService = JournalføringService(repositoryProvider, gatewayProvider),
        utfyllingRepository = repositoryProvider.provide(),
        sakerService = SakerService(repositoryProvider, gatewayProvider),
    )

    override fun utfør(input: JobbInput) {
        val ident = Ident(input.parameter("ident"))
        val utfyllingReferanse = UtfyllingReferanse(UUID.fromString(input.parameter("utfylling")))
        val fagsakReferanse = FagsakReferanse(
            FagsystemNavn.valueOf(input.parameter("fagsak_system")),
            Fagsaknummer(input.parameter("fagsak_nummer"))
        )

        val sak = requireNotNull(
            sakerService.finnSak(
                ident,
                fagsakReferanse
            )
        ) { "Fant ikke sak. Referanse: $fagsakReferanse" }

        journalføringService.journalfør(
            ident = ident,
            utfylling = requireNotNull(
                utfyllingRepository.lastAvsluttetUtfylling(
                    ident,
                    utfyllingReferanse
                )
            ) { "Fant ikke utfylling. Referanse: $utfyllingReferanse" },
            sak = sak,
        )
    }

    companion object {
        private val jobbInfo = object : Jobb {
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
                error("kun for å lage nye jobber")
            }
        }


        fun jobbKonstruktør(repositoryRegistry: RepositoryRegistry) = object : Jobb by jobbInfo {
            override fun konstruer(connection: DBConnection): JobbUtfører {
                return JournalføringJobbUtfører(
                    repositoryRegistry.provider(connection),
                    GatewayProvider,
                )
            }
        }


        fun jobbInput(
            ident: Ident,
            utfylling: UtfyllingReferanse,
            fagsak: FagsakReferanse,
        ): JobbInput {
            return JobbInput(jobbInfo).apply {
                medParameter("ident", ident.asString)
                medParameter("utfylling", utfylling.asUuid.toString())
                medParameter("fagsak_system", fagsak.system.toString())
                medParameter("fagsak_nummer", fagsak.nummer.asString)
            }
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
