package no.nav.aap.varsel

import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.sak.Fagsaknummer
import java.time.Clock
import java.time.Instant

class VarselRepositoryPostgres(private val connection: DBConnection) : VarselRepository {
    override fun hentVarsler(saksnummer: Fagsaknummer): List<Varsel> {
        return connection.queryList(
            """
            select *
            from varsel
            where saksnummer = ?
        """
        ) {
            setParams {
                setString(1, saksnummer.asString)
            }
            setRowMapper {
                mapTilVarsel(it)
            }
        }
    }

    override fun upsert(varsel: Varsel) {
        connection.execute(
            """
            insert into varsel (varsel_id, type_varsel, type_varsel_om, saksnummer, sendingstidspunkt, status, for_periode, opprettet, sist_endret)
            values (?, ?, ?, ?, ?, ?, ?::daterange, ?, ?)
            on conflict (varsel_id)
            do update set
                type_varsel = excluded.type_varsel, 
                type_varsel_om = excluded.type_varsel_om, 
                saksnummer = excluded.saksnummer, 
                sendingstidspunkt = excluded.sendingstidspunkt, 
                status = excluded.status, 
                for_periode = excluded.for_periode, 
                opprettet = excluded.opprettet, 
                sist_endret = excluded.sist_endret
        """
        ) {
            setParams {
                setUUID(1, varsel.varselId.id)
                setEnumName(2, varsel.typeVarsel)
                setEnumName(3, varsel.typeVarselOm)
                setString(4, varsel.saksnummer.asString)
                setInstant(5, varsel.sendingstidspunkt)
                setEnumName(6, varsel.status)
                setPeriode(7, no.nav.aap.komponenter.type.Periode(varsel.forPeriode.fom, varsel.forPeriode.tom))
                setInstant(8, varsel.opprettet)
                setInstant(9, varsel.sistEndret)
            }
        }
    }

    override fun slettPlanlagteVarsler(
        saksnummer: Fagsaknummer,
        typeVarselOm: TypeVarselOm
    ) {
        connection.execute(
            """
            delete from varsel
            where saksnummer = ?
             and status = ?
            and type_varsel_om = ?
            """
        ) {
            setParams {
                setString(1, saksnummer.asString)
                setEnumName(2, VarselStatus.PLANLAGT)
                setEnumName(3, typeVarselOm)
            }
        }
    }

    override fun hentVarslerForUtsending(clock: Clock): List<Varsel> {
        return connection.queryList(
            """
            select *
            from varsel
            where status = ? and sendingstidspunkt <= ?
        """
        ) {
            setParams {
                setEnumName(1, VarselStatus.PLANLAGT)
                setInstant(2, Instant.now(clock))
            }
            setRowMapper {
                mapTilVarsel(it)
            }
        }
    }

    private fun mapTilVarsel(row: Row): Varsel {
        return Varsel(
            varselId = VarselId(row.getUUID("varsel_id")),
            typeVarsel = row.getEnum("type_varsel"),
            typeVarselOm = row.getEnum("type_varsel_om"),
            saksnummer = Fagsaknummer(row.getString("saksnummer")),
            sendingstidspunkt = row.getInstant("sendingstidspunkt"),
            status = row.getEnum("status"),
            forPeriode = row.getPeriode("for_periode").let { dbPeriode -> Periode(dbPeriode.fom, dbPeriode.tom) },
            opprettet = row.getInstant("opprettet"),
            sistEndret = row.getInstant("sist_endret")
        )
    }
}