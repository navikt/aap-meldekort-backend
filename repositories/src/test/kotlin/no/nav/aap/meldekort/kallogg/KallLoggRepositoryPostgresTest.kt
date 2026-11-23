package no.nav.aap.meldekort.kallogg

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kallogg.KallLoggRepository
import no.nav.aap.kallogg.KallLoggStart
import no.nav.aap.kallogg.KallLoggUtfall
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postgresRepositoryRegistry
import no.nav.aap.sak.Fagsaknummer
import org.junit.jupiter.api.AfterEach
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KallLoggRepositoryPostgresTest {
    private val dataSource = TestDataSource()
    private val ident = Ident("12345678901")
    private val today = LocalDate.of(2026, 11, 20)
    private val receivedAt = Instant.parse("2026-11-20T12:00:00Z")

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `stores call against internal person and preserves query values and serialized body`() {
        createCase("123", listOf(ident))
        val entry = entry()
        assertEquals(entry.id, start(ident, entry))
        val response = """{"value":"quoted \"text\"","items":[1,2]}"""
        val completedAt = receivedAt.plusSeconds(1)

        dataSource.transaction { connection ->
            val repository = postgresRepositoryRegistry.provider(connection).provide<KallLoggRepository>()
            repository.complete(entry.id, completedAt, 200, response, KallLoggUtfall.COMPLETED)
        }

        dataSource.transaction { connection ->
            connection.queryFirst(
                """
                select cl.*, kpi.ident, kp.sak_id
                from kallogg cl
                join kelvin_person kp on kp.id = cl.person_id
                join kelvin_person_ident kpi on kpi.person_id = kp.id
                where cl.id = ?
                """
            ) {
                setParams { setUUID(1, entry.id) }
                setRowMapper {
                    assertEquals(ident.asString, it.getString("ident"))
                    assertEquals("BACKEND_CALL", it.getString("entry_type"))
                    assertEquals(receivedAt, it.getInstant("received_at"))
                    assertEquals(completedAt, it.getInstant("completed_at"))
                    assertEquals("GET", it.getString("method"))
                    assertEquals(entry.path, it.getString("path"))
                    assertEquals(
                        entry.queryParameters,
                        DefaultJsonMapper.fromJson<Map<String, List<String>>>(it.getString("query_parameters"))
                    )
                    assertEquals(200, it.getInt("http_status"))
                    assertEquals(response, it.getString("response_body"))
                    assertEquals("COMPLETED", it.getString("outcome"))
                }
            }
        }
    }

    @Test
    fun `ignores unknown identity and cases outside today's date`() {
        assertNull(start(ident, entry()))
        createCase("123", listOf(ident), Periode(today.minusDays(10), today.minusDays(1)))
        assertNull(start(ident, entry()))
        createCase("456", listOf(ident), Periode(today.plusDays(1), today.plusDays(10)))
        assertNull(start(ident, entry()))
        assertEquals(0, count())
    }

    @Test
    fun `resolves historical identifiers to same person and selects case for today`() {
        val previousIdent = Ident("10987654321")
        createCase("old", listOf(ident), Periode(today.minusYears(1), today.minusDays(1)))
        createCase("current", listOf(ident, previousIdent))
        start(ident, entry())
        start(previousIdent, entry())

        dataSource.transaction { connection ->
            val cases = connection.queryList(
                """
                select ks.saksnummer
                from kallogg cl
                join kelvin_person kp on kp.id = cl.person_id
                join kelvin_sak ks on ks.id = kp.sak_id
                """
            ) {
                setRowMapper { it.getString("saksnummer") }
            }
            assertEquals(listOf("current", "current"), cases)
        }
    }

    @Test
    fun `initial record survives unrelated rollback and can complete without a response`() {
        createCase("123", listOf(ident))
        val entry = entry()
        start(ident, entry)
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            dataSource.transaction<Unit> {
                error("Business operation rolled back")
            }
        }
        assertEquals(1, count())
        dataSource.transaction { connection ->
            connection.queryFirst("select outcome, completed_at, http_status, response_body from kallogg") {
                setRowMapper {
                    assertEquals("IN_PROGRESS", it.getString("outcome"))
                    assertNull(it.getInstantOrNull("completed_at"))
                    assertNull(it.getIntOrNull("http_status"))
                    assertNull(it.getStringOrNull("response_body"))
                }
            }
            postgresRepositoryRegistry.provider(connection).provide<KallLoggRepository>()
                .complete(entry.id, receivedAt.plusSeconds(2), null, null, KallLoggUtfall.INTERRUPTED)
        }
        dataSource.transaction { connection ->
            connection.queryFirst("select outcome, http_status, response_body from kallogg") {
                setRowMapper {
                    assertEquals("INTERRUPTED", it.getString("outcome"))
                    assertNull(it.getIntOrNull("http_status"))
                    assertNull(it.getStringOrNull("response_body"))
                }
            }
        }
    }

    private fun entry() = KallLoggStart(
        id = UUID.randomUUID(),
        receivedAt = receivedAt,
        method = "GET",
        path = "/api/meldeperiode/kommende",
        queryParameters = mapOf("repeated" to listOf("first", "second"), "empty" to listOf("")),
    )

    private fun start(ident: Ident, entry: KallLoggStart) = dataSource.transaction { connection ->
        postgresRepositoryRegistry.provider(connection).provide<KallLoggRepository>().start(ident, today, entry)
    }

    private fun count() = dataSource.transaction { connection ->
        connection.queryFirst("select count(*) as count from kallogg") {
            setRowMapper { it.getInt("count") }
        }
    }

    private fun createCase(
        number: String,
        identities: List<Ident>,
        period: Periode = Periode(today, today.plusDays(30)),
    ) {
        dataSource.transaction { connection ->
            KelvinSakRepositoryPostgres(connection).upsertSak(
                saksnummer = Fagsaknummer(number),
                sakenGjelderFor = period,
                identer = identities,
                meldeperioder = emptyList(),
                meldeplikt = emptyList(),
                opplysningsbehov = emptyList(),
                status = null,
                fritak = emptyList(),
            )
        }
    }
}
