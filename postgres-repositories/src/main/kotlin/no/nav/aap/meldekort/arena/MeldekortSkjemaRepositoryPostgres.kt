package no.nav.aap.meldekort.arena

import javax.sql.DataSource
import no.nav.aap.meldekort.arenaflyt.MeldekortSkjemaRepository
import no.nav.aap.meldekort.arenaflyt.MeldekortSkjemaRepositoryFake

class MeldekortSkjemaRepositoryPostgres(
    private val dataSource: DataSource,
): MeldekortSkjemaRepository by MeldekortSkjemaRepositoryFake()


