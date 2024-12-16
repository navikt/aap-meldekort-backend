package no.nav.aap.meldekort.arena

import javax.sql.DataSource
import no.nav.aap.meldekort.arenaflyt.MeldekortRepository
import no.nav.aap.meldekort.arenaflyt.MeldekortRepositoryFake

class MeldekortRepositoryPostgres(
    private val dataSource: DataSource,
): MeldekortRepository by MeldekortRepositoryFake()