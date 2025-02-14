package no.nav.aap.journalføring.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.Ident
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortRepository
import no.nav.aap.arena.MeldekortType
import no.nav.aap.arena.SkjemaRepository
import no.nav.aap.arena.SkjemaTilstand
import no.nav.aap.journalføring.JoarkClient
import no.nav.aap.journalføring.JoarkService
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class ArenaJournalføringJobbUtfører(
    private val skjemaRepository: SkjemaRepository,
    private val meldekortRepository: MeldekortRepository,
    private val joarkService: JoarkService
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(ArenaJournalføringJobbUtfører::class.java)

    override fun utfør(input: JobbInput) {
        log.info("nå utfører vi en jobb")
        val payload = input.payload<ArenaJournalføringJobbPayload>()
        val ident = Ident(payload.ident)
        val meldekortId = MeldekortId(payload.meldekortId)

        val skjema = requireNotNull(skjemaRepository.lastInnsendtSkjema(ident, meldekortId)) {
            "prøver å journalføre meldekort som ikke er i skjema-tabell med meldekortId: ${payload.meldekortId}"
        }
        val meldekort = requireNotNull(meldekortRepository.hent(ident, meldekortId)) {
            "prøver å journalføre meldekort som ikke er i meldekort-tabell med meldekortId: ${payload.meldekortId}"
        }

        val journalpost = joarkService.journalpostForArena(
            skjema = skjema,
            vårReferanse = skjema.referanse.toString(),
            datoMottatt = requireNotNull(skjema.sendtInn?.toLocalDate()) {
                "Prøver å journalføre meldekort som ikke har sendt inn-dato"
            },
            kanSendesFra = meldekort.tidligsteInnsendingsdato,
            korrigert = meldekort.type == MeldekortType.KORRIGERING
        )


        joarkService.journalfør(journalpost)
        skjemaRepository.lagrSkjema(skjema.copy(tilstand = SkjemaTilstand.JOURNALFØRT))
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "journalføringsjobb"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            val joarkService = JoarkService(
                joarkClient = GatewayProvider.provide(JoarkClient::class),
            )

            return ArenaJournalføringJobbUtfører(
                skjemaRepository = repositoryProvider.provide(SkjemaRepository::class),
                meldekortRepository = repositoryProvider.provide(MeldekortRepository::class),
                joarkService = joarkService
            )
        }

        private data class ArenaJournalføringJobbPayload(
            val ident: String,
            val meldekortId: Long,
        )

        fun jobbInput(ident: Ident, meldekortId: MeldekortId): JobbInput {
            return JobbInput(ArenaJournalføringJobbUtfører).medPayload(
                ArenaJournalføringJobbPayload(ident.asString, meldekortId.asLong)
            )
        }

        override fun navn(): String {
            return "journalføring"
        }

        override fun type(): String {
            return "type"
        }
    }
}
