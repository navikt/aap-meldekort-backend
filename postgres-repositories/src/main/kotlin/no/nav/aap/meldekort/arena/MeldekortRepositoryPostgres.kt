package no.nav.aap.meldekort.arena

import javax.sql.DataSource

class MeldekortRepositoryPostgres(
    private val dataSource: DataSource,
): MeldekortRepository by MeldekortRepositoryFake()