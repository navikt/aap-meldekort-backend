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
    private val iDag = LocalDate.of(2026, 11, 20)
    private val mottattTidspunkt = Instant.parse("2026-11-20T12:00:00Z")

    @AfterEach
    fun ryddOpp() {
        dataSource.close()
    }

    @Test
    fun `lagrer ett innslag i kall-logg`() {
        opprettSak("123", listOf(ident))
        val kalloggStart = nyttKalloggStart()
        assertEquals(kalloggStart.id, registrerKall(ident, kalloggStart))
        val response = """{"value":"quoted \"text\"","items":[1,2]}"""
        val fullførtTidspunkt = mottattTidspunkt.plusSeconds(1)

        dataSource.transaction { tilkobling ->
            val kalloggLager = postgresRepositoryRegistry.provider(tilkobling).provide<KallLoggRepository>()
            kalloggLager.complete(kalloggStart.id, fullførtTidspunkt, 200, response, KallLoggUtfall.COMPLETED)
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
                setParams { setUUID(1, kalloggStart.id) }
                setRowMapper { rad ->
                    assertEquals(ident.asString, rad.getString("ident"))
                    assertEquals("BACKEND_CALL", rad.getString("entry_type"))
                    assertEquals(mottattTidspunkt, rad.getInstant("received_at"))
                    assertEquals(fullførtTidspunkt, rad.getInstant("completed_at"))
                    assertEquals("GET", rad.getString("method"))
                    assertEquals(kalloggStart.path, rad.getString("path"))
                    assertEquals(
                        kalloggStart.queryParameters,
                        DefaultJsonMapper.fromJson<Map<String, List<String>>>(rad.getString("query_parameters"))
                    )
                    assertEquals(200, rad.getInt("http_status"))
                    assertEquals(response, rad.getString("response_body"))
                    assertEquals("COMPLETED", rad.getString("outcome"))
                }
            }
        }
    }

    @Test
    fun `uatoriserte brukere og brukere som ikke finnes i system gir ingen logg-innslag`() {
        assertNull(registrerKall(ident, nyttKalloggStart()))
        opprettSak("123", listOf(ident), Periode(iDag.minusDays(10), iDag.minusDays(1)))
        assertNull(registrerKall(ident, nyttKalloggStart()))
        opprettSak("456", listOf(ident), Periode(iDag.plusDays(1), iDag.plusDays(10)))
        assertNull(registrerKall(ident, nyttKalloggStart()))
        assertEquals(0, antallKall())
    }

    @Test
    fun `mapper til gjeldende ident for brukere som har hatt flere identer`() {
        val tidligereIdent = Ident("10987654321")
        opprettSak("old", listOf(ident), Periode(iDag.minusYears(1), iDag.minusDays(1)))
        opprettSak("current", listOf(ident, tidligereIdent))
        registrerKall(ident, nyttKalloggStart())
        registrerKall(tidligereIdent, nyttKalloggStart())

        dataSource.transaction { connection ->
            val saker = connection.queryList(
                """
                select ks.saksnummer
                from kallogg cl
                join kelvin_person kp on kp.id = cl.person_id
                join kelvin_sak ks on ks.id = kp.sak_id
                """
            ) {
                setRowMapper { rad -> rad.getString("saksnummer") }
            }
            assertEquals(listOf("current", "current"), saker)
        }
    }

    @Test
    fun `et kall som blir avbrutt blir logget med status "INTERRUPTED"`() {
        opprettSak("123", listOf(ident))
        val kalloggStart = nyttKalloggStart()
        registrerKall(ident, kalloggStart)
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            dataSource.transaction<Unit> {
                error("Forretningsoperasjonen ble rullet tilbake")
            }
        }
        assertEquals(1, antallKall())
        dataSource.transaction { connection ->
            connection.queryFirst("select outcome, completed_at, http_status, response_body from kallogg") {
                setRowMapper { rad ->
                    assertEquals("IN_PROGRESS", rad.getString("outcome"))
                    assertNull(rad.getInstantOrNull("completed_at"))
                    assertNull(rad.getIntOrNull("http_status"))
                    assertNull(rad.getStringOrNull("response_body"))
                }
            }
            postgresRepositoryRegistry.provider(connection).provide<KallLoggRepository>()
                .complete(kalloggStart.id, mottattTidspunkt.plusSeconds(2), null, null, KallLoggUtfall.INTERRUPTED)
        }
        dataSource.transaction { connection ->
            connection.queryFirst("select outcome, http_status, response_body from kallogg") {
                setRowMapper { rad ->
                    assertEquals("INTERRUPTED", rad.getString("outcome"))
                    assertNull(rad.getIntOrNull("http_status"))
                    assertNull(rad.getStringOrNull("response_body"))
                }
            }
        }
    }

    private fun nyttKalloggStart() = KallLoggStart(
        id = UUID.randomUUID(),
        receivedAt = mottattTidspunkt,
        method = "GET",
        path = "/api/meldeperiode/kommende",
        queryParameters = mapOf("repeated" to listOf("first", "second"), "empty" to listOf("")),
    )

    private fun registrerKall(ident: Ident, kalloggStart: KallLoggStart) = dataSource.transaction { tilkobling ->
        postgresRepositoryRegistry.provider(tilkobling).provide<KallLoggRepository>().start(ident, iDag, kalloggStart)
    }

    private fun antallKall() = dataSource.transaction { tilkobling ->
        tilkobling.queryFirst("select count(*) as count from kallogg") {
            setRowMapper { rad -> rad.getInt("count") }
        }
    }

    private fun opprettSak(
        saksnummer: String,
        identer: List<Ident>,
        periode: Periode = Periode(iDag, iDag.plusDays(30)),
    ) {
        dataSource.transaction { connection ->
            KelvinSakRepositoryPostgres(connection).upsertSak(
                saksnummer = Fagsaknummer(saksnummer),
                sakenGjelderFor = periode,
                identer = identer,
                meldeperioder = emptyList(),
                meldeplikt = emptyList(),
                opplysningsbehov = emptyList(),
                status = null,
                fritak = emptyList(),
            )
        }
    }
}
