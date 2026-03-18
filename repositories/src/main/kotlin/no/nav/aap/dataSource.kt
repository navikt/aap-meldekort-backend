package no.nav.aap

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.dbmigrering.Migrering
import javax.sql.DataSource

class DbConfig(
    val database: String = System.getenv("DB_DATABASE"),
    val url: String = System.getenv("DB_JDBC_URL"),
    val username: String = System.getenv("DB_USERNAME"),
    val password: String = System.getenv("DB_PASSWORD"),
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
        connectionTimeout = 10_000      // 10s to wait for a connection from the pool
        validationTimeout = 5_000       // 5s to validate a connection
        keepaliveTime = 30_000          // 30s keepalive to prevent stale connections
        maxLifetime = 600_000           // 10 min max lifetime for a connection
        initializationFailTimeout = 30_000 // 30s to wait during pool initialization
        metricRegistry = meterRegistry
    })

    Migrering.migrate(dataSource)

    return dataSource
}

