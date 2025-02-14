package no.nav.aap.meldekort

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.dbmigrering.Migrering
import javax.sql.DataSource

class DbConfig(
    val database: String,
    val url: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun fromEnv() = DbConfig(
            database = System.getenv("DB_DATABASE"),
            url = System.getenv("DB_JDBC_URL"),
            username = System.getenv("DB_USERNAME"),
            password = System.getenv("DB_PASSWORD"),
        )
    }
}

fun createPostgresDataSource(dbConfig: DbConfig, meterRegistry: MeterRegistry): DataSource {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.username
        password = dbConfig.password
        maximumPoolSize = 10
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = meterRegistry
    })

    Migrering.migrate(dataSource)

    return dataSource
}

