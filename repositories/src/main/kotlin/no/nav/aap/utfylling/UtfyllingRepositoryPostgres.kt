package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsakReferanse

class UtfyllingRepositoryPostgres(
    private val connection: DBConnection
) : UtfyllingRepository {

    override fun lastÅpenUtfylling(ident: Ident, periode: Periode, utfyllingsflyter: Utfyllingsflyter): Utfylling? {
        return connection.queryFirstOrNull("""
            select * from utfylling
            where ident = ? and periode = ?::daterange
            order by sist_endret desc
            limit 1
        """) {
            setParams {
                setString(1, ident.asString)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(periode.fom, periode.tom))
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
            insert into utfylling(ident, referanse, fagsystem, fagsaknummer, periode, opprettet, sist_endret, flyt, aktivt_steg, avsluttet, svar)
            values (?, ?, ?, ?, ?::daterange, ?, ?, ?, ?, ?, ?::jsonb)
        """
        ) {
            setParams {
                setString(1, utfylling.ident.asString)
                setUUID(2, utfylling.referanse.asUuid)
                setEnumName(3, utfylling.fagsak.system)
                setString(4, utfylling.fagsak.nummer.asString)
                setPeriode(5, no.nav.aap.komponenter.type.Periode(utfylling.periode.fom, utfylling.periode.tom))
                setInstant(6, utfylling.opprettet)
                setInstant(7, utfylling.sistEndret)
                setEnumName(8, utfylling.flyt.navn)
                setEnumName(9, utfylling.aktivtSteg.navn)
                setBoolean(10, utfylling.erAvsluttet)
                setString(11, DefaultJsonMapper.toJson(utfylling.svar))
            }
        }
    }

    private fun utfyllingRowMapper(row: Row, utfyllingsflyter: Utfyllingsflyter): Utfylling {
        val flyt = row.getEnum<UtfyllingFlytNavn>("flyt").let { utfyllingsflyter.flytForNavn(it) }
        return Utfylling(
            ident = Ident(row.getString("ident")),
            referanse = UtfyllingReferanse(row.getUUID("referanse")),
            fagsak = FagsakReferanse(
                system = row.getEnum("fagsystem"),
                nummer = Fagsaknummer(row.getString("fagsaknummer")),
            ),
            periode = row.getPeriode("periode").let { Periode(it.fom, it.tom) },
            flyt = flyt,
            aktivtSteg = row.getEnum<UtfyllingStegNavn>("aktivt_steg").let { flyt.stegForNavn(it) },
            opprettet = row.getInstant("opprettet"),
            sistEndret = row.getInstant("sist_endret"),
            svar = DefaultJsonMapper.fromJson(row.getString("svar")),
        )
    }

    companion object : Factory<UtfyllingRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): UtfyllingRepositoryPostgres {
            return UtfyllingRepositoryPostgres(connection)
        }
    }
}