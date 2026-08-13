package no.nav.aap.statistikk

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory

class StatistikkRepositoryPostgres(private val connection: DBConnection) : StatistikkRepository {

    override fun hentStatistikk(): Statistikk {
        return Statistikk(
            mottatteMeldekortTotalt = queryFirstCount(
                "SELECT COUNT(*) AS count FROM utfylling WHERE avsluttet = true"
            ),
            mottatteMeldekortIDag = queryFirstCount(
                "SELECT COUNT(*) AS count FROM utfylling WHERE avsluttet = true AND sist_endret::date = CURRENT_DATE"
            ),
            varslerSendtIDag = queryFirstCount(
                "SELECT COUNT(*) AS count FROM varsel WHERE status = 'SENDT' AND sist_endret::date = CURRENT_DATE"
            ),
            varslerInaktivertIDag = queryFirstCount(
                "SELECT COUNT(*) AS count FROM varsel WHERE status = 'INAKTIVERT' AND sist_endret::date = CURRENT_DATE"
            ),
            varslerPlanlagt = queryFirstCount(
                "SELECT COUNT(*) AS count FROM varsel WHERE status = 'PLANLAGT' AND for_periode @> CURRENT_DATE"
            ),
        )
    }

    private fun queryFirstCount(sql: String): Long {
        return connection.queryFirst(sql) {
            setRowMapper { it.getLong("count") }
        }
    }

    companion object : RepositoryFactory<StatistikkRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): StatistikkRepositoryPostgres {
            return StatistikkRepositoryPostgres(connection)
        }
    }
}
