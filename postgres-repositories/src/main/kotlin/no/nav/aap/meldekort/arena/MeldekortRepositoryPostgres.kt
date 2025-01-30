package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.Periode
import no.nav.aap.komponenter.type.Periode as DbPeriode

class MeldekortRepositoryPostgres(private val connection: DBConnection) : MeldekortRepository {
    companion object : Factory<MeldekortRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): MeldekortRepositoryPostgres {
            return MeldekortRepositoryPostgres(connection)
        }
    }

    override fun upsert(ident: Ident, meldekort: List<Meldekort>) {
        connection.executeBatch(
            """
                    insert into arena_meldekort (
                        ident,
                        meldekort_id,
                        kan_korrigeres,
                        periode,
                        type,
                        tilstand,
                        begrunnelse_endring,
                        mottatt,
                        original_meldekort_id,
                        beregning_status,
                        brutto_beløp
                    )
                    values (?, ?, ?, ?::daterange, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (ident, meldekort_id)
                    do update set
                        kan_korrigeres = excluded.kan_korrigeres,
                        periode = excluded.periode,
                        type = excluded.type,
                        tilstand = excluded.tilstand,
                        begrunnelse_endring = excluded.begrunnelse_endring,
                        mottatt = coalesce(excluded.mottatt, arena_meldekort.mottatt),
                        original_meldekort_id = coalesce(excluded.original_meldekort_id, arena_meldekort.original_meldekort_id),
                        beregning_status = excluded.beregning_status,
                        brutto_beløp = excluded.brutto_beløp
        """.trimIndent(),
            meldekort,
        ) {
            setParams {
                setString(1, ident.asString)
                setLong(2, it.meldekortId.asLong)
                setBoolean(3, it.kanKorrigeres)
                setPeriode(4, DbPeriode(it.periode.fom, it.periode.tom))
                setEnumName(5, it.type)
                when (it) {
                    is KommendeMeldekort -> {
                        setEnumName(6, Tilstand.KOMMENDE)
                        setString(7, null)
                        setLocalDate(8, null)
                        setLong(9, null)
                        setEnumName(10, null)
                        setDouble(11, null)
                    }

                    is HistoriskMeldekort -> {
                        setEnumName(6, Tilstand.HISTORISK)
                        setString(7, it.begrunnelseEndring)
                        setLocalDate(8, it.mottattIArena)
                        setLong(9, it.originalMeldekortId?.asLong)
                        setEnumName(10, it.beregningStatus)
                        setDouble(11, it.bruttoBeløp)
                    }
                }
            }
        }
    }

    override fun hent(ident: Ident, meldekortId: MeldekortId): Meldekort? {
        return connection.queryFirstOrNull("select * from arena_meldekort where ident = ? and meldekort_id = ?") {
            setParams {
                setString(1, ident.asString)
                setLong(2, meldekortId.asLong)
            }
            setRowMapper {
                mapTilMeldekort(it)
            }
        }
    }


    override fun hent(ident: Ident, meldekortId: List<MeldekortId>): List<Meldekort> {
        return connection.queryList("select * from arena_meldekort where ident = ? and meldekort_id = any(?::bigint[])") {
            setParams {
                setString(1, ident.asString)
                setLongArray(2, meldekortId.map { it.asLong })
            }
            setRowMapper {
                mapTilMeldekort(it)
            }
        }
    }

    private fun mapTilMeldekort(row: Row): Meldekort {
        return when (row.getEnum<Tilstand>("tilstand")) {
            Tilstand.KOMMENDE -> KommendeMeldekort(
                meldekortId = MeldekortId(row.getLong("meldekort_id")),
                type = row.getEnum("type"),
                periode = row.getPeriode("periode").let { dbPeriode -> Periode(dbPeriode.fom, dbPeriode.tom) },
                kanKorrigeres = row.getBoolean("kan_korrigeres"),
            )

            Tilstand.HISTORISK -> mapTilHistoriskeMeldekort(row)
        }
    }

    private fun mapTilHistoriskeMeldekort(row: Row): HistoriskMeldekort {
        return HistoriskMeldekort(
            meldekortId = MeldekortId(row.getLong("meldekort_id")),
            type = row.getEnum("type"),
            periode = row.getPeriode("periode").let { dbPeriode -> Periode(dbPeriode.fom, dbPeriode.tom) },
            kanKorrigeres = row.getBoolean("kan_korrigeres"),
            begrunnelseEndring = row.getStringOrNull("begrunnelse_endring"),
            mottattIArena = row.getLocalDateOrNull("mottatt"),
            originalMeldekortId = row.getLongOrNull("original_meldekort_id")?.let(::MeldekortId),
            beregningStatus = row.getEnum("beregning_status"),
            bruttoBeløp = row.getDoubleOrNull("brutto_beløp")
        )
    }

    override fun hentAlleHistoriskeMeldekort(ident: Ident): List<HistoriskMeldekort> {
        return connection.queryList("select * from arena_meldekort where ident = ? and tilstand = ?") {
            setParams {
                setString(1, ident.asString)
                setEnumName(2, Tilstand.HISTORISK)
            }
            setRowMapper { mapTilHistoriskeMeldekort(it) }
        }
    }

    private enum class Tilstand {
        KOMMENDE, HISTORISK
    }
}