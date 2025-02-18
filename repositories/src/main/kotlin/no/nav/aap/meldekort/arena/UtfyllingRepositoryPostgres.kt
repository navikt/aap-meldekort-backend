package no.nav.aap.meldekort.arena

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepository
import no.nav.aap.utfylling.UtfyllingStegNavn
import no.nav.aap.utfylling.Utfyllingsflyter
import no.nav.aap.komponenter.type.Periode as DbPeriode

class UtfyllingRepositoryPostgres(
    private val connection: DBConnection
) : UtfyllingRepository {
    companion object : Factory<UtfyllingRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): UtfyllingRepositoryPostgres {
            return UtfyllingRepositoryPostgres(connection)
        }
    }

    override fun lastÅpenUtfylling(ident: Ident, periode: Periode, utfyllingsflyter: Utfyllingsflyter): Utfylling? {
        return connection.queryFirstOrNull("""
            select * from utfylling
            where ident = ? and periode = ?::daterange
            order by sist_endret desc
            limit 1
        """) {
            setParams {
                setString(1, ident.asString)
                setPeriode(2, DbPeriode(periode.fom, periode.tom))
            }
            setRowMapper { row ->
                utfyllingRowMapper(row, utfyllingsflyter)
            }
        }
            ?.takeUnless { it.erAvsluttet }
    }

    override fun lastUtfylling(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
        utfyllingsflyter: Utfyllingsflyter
    ): Utfylling? {
        return connection.queryFirstOrNull("""
            select * from utfylling
            where ident = ? and referanse = ?
            order by sist_endret desc
            limit 1
        """) {
            setParams {
                setString(1, ident.asString)
                setUUID(2, utfyllingReferanse.asUuid)
            }
            setRowMapper { row ->
                utfyllingRowMapper(row, utfyllingsflyter)
            }
        }
    }

    override fun lagrUtfylling(utfylling: Utfylling) {
        connection.execute(
            """
            insert into utfylling(ident, referanse, periode, opprettet, sist_endret, flyt, aktivt_steg, avsluttet, svar)
            values (?, ?, ?::daterange, ?, ?, ?, ?, ?, ?::jsonb)
        """
        ) {
            setParams {
                setString(1, utfylling.ident.asString)
                setUUID(2, utfylling.referanse.asUuid)
                setPeriode(3, DbPeriode(utfylling.periode.fom, utfylling.periode.tom))
                setInstant(4, utfylling.opprettet)
                setInstant(5, utfylling.sistEndret)
                setEnumName(6, utfylling.flyt.navn)
                setEnumName(7, utfylling.aktivtSteg.navn)
                setBoolean(8, utfylling.erAvsluttet)
                setString(9, DefaultJsonMapper.toJson(utfylling.svar))
            }
        }
    }

    private fun utfyllingRowMapper(row: Row, utfyllingsflyter: Utfyllingsflyter): Utfylling {
        val flyt = row.getEnum<UtfyllingFlytNavn>("flyt").let { utfyllingsflyter.flytForNavn(it) }
        return Utfylling(
            ident = Ident(row.getString("ident")),
            referanse = UtfyllingReferanse(row.getUUID("referanse")),
            periode = row.getPeriode("periode").let { Periode(it.fom, it.tom) },
            flyt = flyt,
            aktivtSteg = row.getEnum<UtfyllingStegNavn>("aktivt_steg").let { flyt.stegForNavn(it) },
            opprettet = row.getInstant("opprettet"),
            sistEndret = row.getInstant("sist_endret"),
            svar = DefaultJsonMapper.fromJson(row.getString("svar")),
        )
    }
}


