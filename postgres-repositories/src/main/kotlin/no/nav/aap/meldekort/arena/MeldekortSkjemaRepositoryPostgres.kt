package no.nav.aap.meldekort.arena

import javax.sql.DataSource

class MeldekortSkjemaRepositoryPostgres(
    private val dataSource: DataSource,
): MeldekortSkjemaRepository by MeldekortSkjemaRepositoryFake()


