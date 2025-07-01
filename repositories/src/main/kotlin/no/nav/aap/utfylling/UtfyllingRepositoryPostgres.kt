package no.nav.aap.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsakReferanse
import java.time.LocalDate

class UtfyllingRepositoryPostgres(
    private val connection: DBConnection
) : UtfyllingRepository {

    override fun lastAvsluttetUtfylling(ident: Ident, utfyllingReferanse: UtfyllingReferanse): Utfylling? {
        return lastUtfylling(ident, utfyllingReferanse)
            ?.takeIf { it.erAvsluttet }
    }

    override fun lastÅpenUtfylling(ident: Ident, periode: Periode): Utfylling? {
        return connection.queryFirstOrNull(
            """
            select * from utfylling
            where ident = ? and periode = ?::daterange
            order by sist_endret desc
            limit 1
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(periode.fom, periode.tom))
            }
            setRowMapper { row ->
                utfyllingRowMapper(row)
            }
        }
            ?.takeUnless { it.erAvsluttet }
    }

    override fun lastUtfylling(
        ident: Ident,
        utfyllingReferanse: UtfyllingReferanse,
    ): Utfylling? {
        return connection.queryFirstOrNull(
            """
            select * from utfylling
            where ident = ? and referanse = ?
            order by sist_endret desc
            limit 1
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setUUID(2, utfyllingReferanse.asUuid)
            }
            setRowMapper { row ->
                utfyllingRowMapper(row)
            }
        }
    }

    override fun hentUtfyllinger(saksnummer: Fagsaknummer): List<Utfylling> {
        return connection.queryList(
            """
                select *
                from utfylling
                where utfylling.fagsaknummer = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, saksnummer.asString)
            }
            setRowMapper {
                utfyllingRowMapper(it)
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
                setEnumName(8, utfylling.flyt)
                setEnumName(9, utfylling.aktivtSteg)
                setBoolean(10, utfylling.erAvsluttet)
                setString(11, DefaultJsonMapper.toJson(utfylling.svar))
            }
        }
    }

    override fun slettUtkast(ident: Ident, utfyllingReferanse: UtfyllingReferanse) {
        connection.execute(
            """
        DELETE FROM utfylling WHERE ident = ? and referanse = ? AND NOT avsluttet
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setUUID(2, utfyllingReferanse.asUuid)
            }
        }
    }

    override fun slettGamleUtkast(slettTilOgMed: LocalDate) {
        connection.execute("""
            delete from utfylling
            where opprettet::date <= ? and avsluttet = false
        """.trimIndent()) {
            setParams {
                setLocalDate(1, slettTilOgMed)
            }
        }
    }

    private fun utfyllingRowMapper(row: Row): Utfylling {
        val flyt = row.getEnum<UtfyllingFlytNavn>("flyt")
        return Utfylling(
            ident = Ident(row.getString("ident")),
            referanse = UtfyllingReferanse(row.getUUID("referanse")),
            fagsak = FagsakReferanse(
                system = row.getEnum("fagsystem"),
                nummer = Fagsaknummer(row.getString("fagsaknummer")),
            ),
            periode = row.getPeriode("periode").let { Periode(it.fom, it.tom) },
            flyt = flyt,
            aktivtSteg = row.getEnum<UtfyllingStegNavn>("aktivt_steg"),
            opprettet = row.getInstant("opprettet"),
            sistEndret = row.getInstant("sist_endret"),
            svar = DefaultJsonMapper.fromJson(row.getString("svar")),
        )
    }

    companion object : RepositoryFactory<UtfyllingRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): UtfyllingRepositoryPostgres {
            return UtfyllingRepositoryPostgres(connection)
        }
    }
}