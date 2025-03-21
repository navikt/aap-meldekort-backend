package no.nav.aap.arena

//import no.nav.aap.komponenter.dbconnect.DBConnection
//import no.nav.aap.komponenter.dbconnect.Row
//import no.nav.aap.lookup.repository.Factory
//import no.nav.aap.Ident
//import no.nav.aap.Periode
//import java.time.LocalDateTime
//import no.nav.aap.utfylling.Svar
//import no.nav.aap.arena.MeldekortId
//import no.nav.aap.meldekort.Skjema
//import no.nav.aap.meldekort.SkjemaId
//import no.nav.aap.meldekort.SkjemaRepository
//import no.nav.aap.utfylling.TimerArbeidet
//import no.nav.aap.komponenter.type.Periode as dbPeriode
//
//class SkjemaRepositoryPostgres(private val connection: DBConnection) : SkjemaRepository {
//    companion object : Factory<SkjemaRepositoryPostgres> {
//        override fun konstruer(connection: DBConnection): SkjemaRepositoryPostgres {
//            return SkjemaRepositoryPostgres(connection)
//        }
//    }
//
//    fun last(skjemaId: SkjemaId): Skjema {
//        return connection.queryFirst("select * from arena_skjema where id = ?") {
//            setParams {
//                setLong(1, skjemaId.asLong)
//            }
//            setRowMapper(::mapSkjema)
//        }
//    }
//
//    override fun last(ident: Ident, meldekortId: MeldekortId): Skjema? {
//        return connection.queryFirstOrNull(
//            "select * from arena_skjema where ident = ? and meldekort_id = ? order by tid_opprettet desc limit 1"
//        ) {
//            setParams {
//                setString(1, ident.asString)
//                setLong(2, meldekortId.asLong)
//            }
//            setRowMapper(::mapSkjema)
//        }
//    }
//
//    override fun lastInnsendtSkjema(ident: Ident, meldekortId: MeldekortId): Skjema? {
//        return connection.queryFirstOrNull("""
//            select * from arena_skjema
//            where ident = ? and meldekort_id = ? and sendt_inn is not null
//            order by tid_opprettet desc limit 1
//        """) {
//            setParams {
//                setString(1, ident.asString)
//                setLong(2, meldekortId.asLong)
//            }
//            setRowMapper(::mapSkjema)
//        }
//    }
//
//    private fun mapSkjema(row: Row): Skjema {
//        return Skjema(
//            tilstand = row.getEnum("tilstand"),
//            meldekortId = MeldekortId(row.getLong("meldekort_id")),
//            ident = Ident(row.getString("ident")),
//            meldeperiode = row.getPeriode("meldeperiode").let { Periode(it.fom, it.tom) },
//            sendtInn = row.getLocalDateTimeOrNull("sendt_inn"),
//            referanse = row.getUUID("referanse"),
//            svar = Svar(
//                svarerDuSant = row.getBooleanOrNull("payload_svarer_du_sant"),
//                harDuJobbet = row.getBooleanOrNull("payload_har_du_jobbet"),
//                timerArbeidet = mapTilTimerArbeidet(row.getLong("id")),
//                stemmerOpplysningene = row.getBooleanOrNull("payload_stemmer_opplysningene"),
//            ),
//        )
//    }
//
//    private fun mapTilTimerArbeidet(skjemaId: Long): List<TimerArbeidet> {
//        return connection.queryList("select * from arena_skjema_timer_arbeidet where skjema_id = ?") {
//            setParams { setLong(1, skjemaId) }
//            setRowMapper { row ->
//                TimerArbeidet(
//                    timer = row.getDoubleOrNull("timer_arbeidet"),
//                    dato = row.getLocalDate("dato"),
//                )
//            }
//        }
//    }
//
//    override fun lagrSkjema(skjema: Skjema): SkjemaId {
//        val skjemaId = connection.executeReturnKey(
//            """
//                insert into arena_skjema (
//                    ident,
//                    tilstand,
//                    meldekort_id,
//                    meldeperiode,
//                    sendt_inn,
//                    referanse,
//                    payload_svarer_du_sant,
//                    payload_har_du_jobbet,
//                    payload_stemmer_opplysningene,
//                    tid_opprettet
//                ) values (?, ?, ?, ?::daterange, ?, ?, ?, ?, ?, ?)
//            """.trimIndent()
//        ) {
//            setParams {
//                setString(1, skjema.ident.asString)
//                setEnumName(2, skjema.tilstand)
//                setLong(3, skjema.meldekortId.asLong)
//                setPeriode(4, skjema.meldeperiode.let { dbPeriode(it.fom, it.tom) })
//                setLocalDateTime(5, skjema.sendtInn)
//                setUUID(6, skjema.referanse)
//                setBoolean(7, skjema.svar.svarerDuSant)
//                setBoolean(8, skjema.svar.harDuJobbet)
//                setBoolean(9, skjema.svar.stemmerOpplysningene)
//                setLocalDateTime(10, LocalDateTime.now())
//            }
//        }
//
//        lagreTimerArbeidet(skjemaId, skjema.svar.timerArbeidet)
//        return SkjemaId(skjemaId)
//    }
//
//    private fun lagreTimerArbeidet(skjemaId: Long, timerArbeidet: List<TimerArbeidet>) {
//        connection.executeBatch(
//            "insert into arena_skjema_timer_arbeidet (skjema_id, dato, timer_arbeidet) values (?, ?, ?)",
//            timerArbeidet
//        ) {
//            setParams {
//                setLong(1, skjemaId)
//                setLocalDate(2, it.dato)
//                setDouble(3, it.timer)
//            }
//        }
//    }
//}
//
//
//