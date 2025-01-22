package no.nav.aap.meldekort.arena

import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.meldekort.Ident

class UtfyllingRepositoryPostgres(private val dataSource: DataSource) : UtfyllingRepository {
    private val skjemaRepository = SkjemaRepositoryPostgres(dataSource)

    override fun last(ident: Ident, meldekortId: Long, utfyllingFlyt: UtfyllingFlyt): Utfylling? {
        return dataSource.transaction { connection ->
            connection.queryFirstOrNull(
                "select * from arena_utfylling where ident = ? and meldekort_id = ? order by tid_opprettet desc limit 1"
            ) {
                setParams {
                    setString(1, ident.asString)
                    setLong(2, meldekortId)
                }
                setRowMapper { row ->
                    Utfylling(
                        flyt = utfyllingFlyt,
                        steg = utfyllingFlyt.stegForNavn(row.getEnum("steg")),
                        skjema = skjemaRepository.last(connection, SkjemaId(row.getLong("skjema_id"))),
                        ident = Ident(row.getString("ident")),
                        meldekortId = row.getLong("meldekort_id"),
                    )
                }
            }
        }
    }

    override fun lagrUtfylling(utfylling: Utfylling) {
        dataSource.transaction { connection ->
            val skjemaId = skjemaRepository.lagrSkjema(connection, utfylling.skjema)

            connection.executeReturnKey(
                """
                    insert into arena_utfylling(
                        ident,
                        flyt,
                        meldekort_id,
                        steg,
                        skjema_id
                    ) values (?, ?, ?, ?, ?)
                """.trimIndent()
            ) {
                setParams {
                    setString(1, utfylling.ident.asString)
                    setString(2, utfylling.flyt.toString())
                    setLong(3, utfylling.skjema.meldekortId)
                    setEnumName(4, utfylling.steg.navn)
                    setLong(5, skjemaId.asLong)
                }
            }
        }
    }
}


