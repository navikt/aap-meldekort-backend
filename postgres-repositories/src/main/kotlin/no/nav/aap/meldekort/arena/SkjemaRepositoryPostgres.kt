package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.Periode
import javax.sql.DataSource
import no.nav.aap.komponenter.type.Periode as dbPeriode

class SkjemaRepositoryPostgres(private val dataSource: DataSource) : SkjemaRepository {
    override fun last(ident: Ident, meldekortId: Long, skjemaFlyt: SkjemaFlyt): Skjema? {
        return dataSource.transaction { connection ->
            connection.queryFirstOrNull(
                "select * from arena_skjema where ident = ? and meldekort_id = ? order by tid_opprettet desc limit 1"
            ) {
                setParams {
                    setString(1, ident.asString)
                    setLong(2, meldekortId)
                }
                setRowMapper { row ->
                    Skjema(
                        flyt = skjemaFlyt,
                        tilstand = row.getEnum("tilstand"),
                        meldekortId = row.getLong("meldekort_id"),
                        ident = Ident(row.getString("ident")),
                        steg = skjemaFlyt.stegForNavn(row.getEnum("steg")),
                        meldeperiode = row.getPeriode("meldeperiode").let { Periode(it.fom, it.tom) },
                        payload = InnsendingPayload(
                            svarerDuSant = row.getBooleanOrNull("payload_svarer_du_sant"),
                            harDuJobbet = row.getBooleanOrNull("payload_har_du_jobbet"),
                            timerArbeidet = connection.mapTilTimerArbeidet(row.getLong("id")),
                            stemmerOpplysningene = row.getBooleanOrNull("payload_stemmer_opplysningene"),
                        )
                    )
                }
            }
        }
    }

    private fun DBConnection.mapTilTimerArbeidet(skjemaId: Long): List<TimerArbeidet> {
        return queryList("select * from arena_skjema_timer_arbeidet where skjema_id = ?") {
            setParams { setLong(1, skjemaId) }
            setRowMapper { row ->
                TimerArbeidet(
                    timer = row.getDoubleOrNull("timer_arbeidet"),
                    dato = row.getLocalDate("dato"),
                )
            }
        }
    }

    override fun lagrSkjema(skjema: Skjema) {
        dataSource.transaction { connection ->
            val skjemaId = connection.executeReturnKey(
                """
                    insert into arena_skjema (
                        ident,
                        flyt,
                        tilstand,
                        meldekort_id,
                        steg,
                        meldeperiode,
                        payload_svarer_du_sant,
                        payload_har_du_jobbet,
                        payload_stemmer_opplysningene
                    ) values (?, ?, ?, ?, ?, ?::daterange, ?, ?, ?)
                """.trimIndent()
            ) {
                setParams {
                    setString(1, skjema.ident.asString)
                    setString(2, skjema.flyt.toString())
                    setEnumName(3, skjema.tilstand)
                    setLong(4, skjema.meldekortId)
                    setEnumName(5, skjema.steg.navn)
                    setPeriode(6, skjema.meldeperiode.let { dbPeriode(it.fom, it.tom) })
                    setBoolean(7, skjema.payload.svarerDuSant)
                    setBoolean(8, skjema.payload.harDuJobbet)
                    setBoolean(9, skjema.payload.stemmerOpplysningene)
                }
            }

            connection.lagreTimerArbeidet(skjemaId, skjema.payload.timerArbeidet)
        }
    }

    private fun DBConnection.lagreTimerArbeidet(skjemaId: Long, timerArbeidet: List<TimerArbeidet>) {
        executeBatch(
            "insert into arena_skjema_timer_arbeidet (skjema_id, dato, timer_arbeidet) values (?, ?, ?)",
            timerArbeidet
        ) {
           setParams {
               setLong(1, skjemaId)
               setLocalDate(2, it.dato)
               setDouble(3, it.timer)
           }
        }
    }

}


