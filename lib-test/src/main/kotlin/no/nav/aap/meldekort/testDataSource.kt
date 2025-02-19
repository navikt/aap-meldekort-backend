package no.nav.aap.meldekort

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.DbConfig
import no.nav.aap.createPostgresDataSource
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

fun createTestcontainerPostgresDataSource(meterRegistry: MeterRegistry): DataSource {
    val postgres = postgreSQLContainer()
    // Useful for connecting to the test database locally
    // jdbc 'URL contains the host and port and database name.
    println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")

    val dbConfig = DbConfig(
        database = "sdf",
        url = postgres.jdbcUrl,
        username = postgres.username,
        password = postgres.password
    )
    return createPostgresDataSource(dbConfig, meterRegistry)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}
