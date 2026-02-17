package no.nav.aap.meldekort

import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.DbConfig
import no.nav.aap.createPostgresDataSource
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

fun createTestcontainerPostgresDataSource(meterRegistry: MeterRegistry): DataSource {
    val dbConfig = if (System.getenv("DB_JDBC_URL").isNullOrBlank()) {
        val postgres = postgreSQLContainer()

        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")

        DbConfig(
            database = "sdf",
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
    } else {
        DbConfig()
    }
    // Useful for connecting to the test database locally
    // jdbc 'URL contains the host and port and database name.
    return createPostgresDataSource(dbConfig, meterRegistry)
}

private fun postgreSQLContainer(): PostgreSQLContainer {
    val postgres = PostgreSQLContainer("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}