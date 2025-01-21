package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import javax.sql.DataSource

class SkjemaRepositoryPostgres(
    private val dataSource: DataSource,
): SkjemaRepository by SkjemaRepositoryFake()


