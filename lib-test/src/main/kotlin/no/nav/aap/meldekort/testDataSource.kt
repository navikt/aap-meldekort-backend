package no.nav.aap.meldekort

import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.DbConfig
import no.nav.aap.createPostgresDataSource
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.sql.DataSource
import kotlin.String

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
        DbConfig(database = "sdf")
    }
    // Useful for connecting to the test database locally
    // jdbc 'URL contains the host and port and database name.
    return createPostgresDataSource(dbConfig, meterRegistry)
}

private fun postgreSQLContainer(): PostgreSQLContainer {
    val postgres = PostgreSQLContainer("postgres:16")
    postgres.waitingFor(
        Wait.forListeningPort().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS))
    )
    postgres.start()
    waitForDatabaseReady(postgres)
    return postgres
}

private fun waitForDatabaseReady(postgres: PostgreSQLContainer, maxAttempts: Int = 30) {
    repeat(maxAttempts) { attempt ->
        try {
            java.sql.DriverManager.getConnection(
                postgres.jdbcUrl, postgres.username, postgres.password
            ).use { connection ->
                connection.createStatement().use { it.execute("SELECT 1") }
            }
            return
        } catch (e: Exception) {
            if (attempt == maxAttempts - 1) throw e
            Thread.sleep(500)
        }
    }
}
