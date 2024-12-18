package no.nav.aap.meldekort.arena

import javax.sql.DataSource

class SkjemaRepositoryPostgres(
    private val dataSource: DataSource,
): SkjemaRepository by SkjemaRepositoryFake()


