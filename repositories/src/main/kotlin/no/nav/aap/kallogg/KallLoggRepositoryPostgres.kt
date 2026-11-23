package no.nav.aap.kallogg

import no.nav.aap.Ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class KallLoggRepositoryPostgres(private val connection: DBConnection) : KallLoggRepository {
    override fun start(ident: Ident, paaDag: LocalDate, entry: KallLoggStart): UUID? {
        val personId = connection.queryFirstOrNull<Long>(
            """
            select kelvin_person.id
            from kelvin_sak
            join kelvin_person on kelvin_sak.id = kelvin_person.sak_id
            join kelvin_person_ident on kelvin_person.id = kelvin_person_ident.person_id
            where kelvin_person_ident.ident = ? and kelvin_sak.saken_gjelder_for @> ?::date
            """
        ) {
            setParams {
                setString(1, ident.asString)
                setLocalDate(2, paaDag)
            }
            setRowMapper { it.getLong("id") }
        } ?: return null

        connection.execute(
            """
            insert into kallogg (
                id, person_id, entry_type, received_at, method, path, query_parameters, outcome
            ) values (?, ?, 'BACKEND_CALL', ?, ?, ?, ?::jsonb, 'IN_PROGRESS')
            """
        ) {
            setParams {
                setUUID(1, entry.id)
                setLong(2, personId)
                setInstant(3, entry.receivedAt)
                setString(4, entry.method)
                setString(5, entry.path)
                setString(6, DefaultJsonMapper.toJson(entry.queryParameters))
            }
        }
        return entry.id
    }

    override fun complete(
        id: UUID,
        completedAt: Instant,
        status: Int?,
        responseBody: String?,
        outcome: KallLoggUtfall,
    ) {
        require(outcome != KallLoggUtfall.IN_PROGRESS)
        connection.execute(
            """
            update kallogg
            set completed_at = ?, http_status = ?, response_body = ?, outcome = ?
            where id = ?
            """
        ) {
            setParams {
                setInstant(1, completedAt)
                setInt(2, status)
                setString(3, responseBody)
                setEnumName(4, outcome)
                setUUID(5, id)
            }
            setResultValidator { require(it == 1) }
        }
    }

    companion object : RepositoryFactory<KallLoggRepositoryPostgres> {
        override fun konstruer(connection: DBConnection) = KallLoggRepositoryPostgres(connection)
    }
}
