package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository

class UtfyllingRepositoryPostgres(private val connection: DBConnection) : UtfyllingRepository {
    companion object : Factory<UtfyllingRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): UtfyllingRepositoryPostgres {
            return UtfyllingRepositoryPostgres(connection)
        }
    }

    override fun lastÅpenUtfylling(ident: Ident, periode: Periode): Utfylling? {
        return null
    }

    override fun lastUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling? {
        return null
    }

    override fun lagrUtfylling(utfylling: Utfylling) {
    }

//    private val skjemaRepository = SkjemaRepositoryPostgres(connection)
//
//    override fun last(ident: Ident, meldekortId: MeldekortId, utfyllingFlytOrkestrator: UtfyllingFlytOrkestrator): Utfylling? {
//        return connection.queryFirstOrNull(
//            "select * from arena_utfylling where ident = ? and meldekort_id = ? order by tid_opprettet desc limit 1"
//        ) {
//            setParams {
//                setString(1, ident.asString)
//                setLong(2, meldekortId.asLong)
//            }
//            setRowMapper { row ->
//                Utfylling(
//                    flyt = utfyllingFlytOrkestrator,
//                    aktivtSteg = utfyllingFlytOrkestrator.stegForNavn(row.getEnum("steg")),
//                    skjema = skjemaRepository.last(SkjemaId(row.getLong("skjema_id"))),
//                    ident = Ident(row.getString("ident")),
//                    meldekortId = MeldekortId(row.getLong("meldekort_id")),
//                )
//            }
//        }
//    }
//
//    override fun lagrUtfylling(utfylling: Utfylling) {
//        val skjemaId = skjemaRepository.lagrSkjema(utfylling.skjema)
//
//        connection.executeReturnKey(
//            """
//                    insert into arena_utfylling(
//                        ident,
//                        flyt,
//                        meldekort_id,
//                        steg,
//                        skjema_id,
//                        tid_opprettet
//                    ) values (?, ?, ?, ?, ?, ?)
//                """.trimIndent()
//        ) {
//            setParams {
//                setString(1, utfylling.ident.asString)
//                setString(2, utfylling.flyt.toString())
//                setLong(3, utfylling.skjema.meldekortId.asLong)
//                setEnumName(4, utfylling.aktivtSteg.navn)
//                setLong(5, skjemaId.asLong)
//                setLocalDateTime(6, LocalDateTime.now())
//            }
//        }
//    }
}


