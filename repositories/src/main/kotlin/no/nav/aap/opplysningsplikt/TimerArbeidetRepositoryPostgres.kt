package no.nav.aap.opplysningsplikt

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate
import no.nav.aap.komponenter.type.Periode as DBPeriode

class TimerArbeidetRepositoryPostgres(
    private val connection: DBConnection,
) : TimerArbeidetRepository{

    override fun lagrTimerArbeidet(ident: Ident, opplysninger: List<TimerArbeidet>) {
        connection.executeBatch(
            """
                insert into timer_arbeidet
                (ident, registreringstidspunkt, utfylling_referanse, fagsak_system, fagsak_nummer, dato, timer_arbeidet)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict do nothing
            """,
            opplysninger,
        ) {
            setParams {
                it.apply {
                    setString(1, ident.asString)
                    setInstant(2, registreringstidspunkt)
                    setUUID(3, utfylling.asUuid)
                    setEnumName(4, fagsak.system)
                    setString(5, fagsak.nummer.asString)
                    setLocalDate(6, dato)
                    setDouble(7, timerArbeidet)
                }
            }
        }
    }

    override fun hentSenesteOpplysningsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate? {
        return connection.queryFirstOrNull("""
            select dato from timer_arbeidet
            where ident = ? and fagsak_system = ? and fagsak_nummer = ?
            order by dato desc
            limit 1
        """) {
            setParams {
                setString(1, ident.asString)
                setEnumName(2, fagsak.system)
                setString(3, fagsak.nummer.asString)
            }
            setRowMapper { row ->
                row.getLocalDate("dato")
            }
        }
    }

    override fun hentTimerArbeidet(ident: Ident, sak: FagsakReferanse, periode: Periode): List<TimerArbeidet> {
        return connection.queryList("""
            select distinct on (dato) *
            from timer_arbeidet
            where ident = ? and fagsak_system = ? and fagsak_nummer = ? and ?::daterange @> dato
            order by dato, registreringstidspunkt desc
        """) {
            setParams {
                setString(1, ident.asString)
                setEnumName(2, sak.system)
                setString(3, sak.nummer.asString)
                setPeriode(4, DBPeriode(periode.fom, periode.tom))
            }
            setRowMapper(::timerArbeidetRowMapper)
        }
    }
    
    private fun timerArbeidetRowMapper(row: Row): TimerArbeidet {
        return TimerArbeidet(
            registreringstidspunkt = row.getInstant("registreringstidspunkt"),
            utfylling = UtfyllingReferanse(row.getUUID("utfylling_referanse")),
            fagsak = FagsakReferanse(
                system = row.getEnum("fagsak_system"),
                nummer = Fagsaknummer(row.getString("fagsak_nummer")),
            ),
            dato = row.getLocalDate("dato"),
            timerArbeidet = row.getDoubleOrNull("timer_arbeidet")
        )
    }

    companion object : Factory<TimerArbeidetRepositoryPostgres> {
        override fun konstruer(connection: DBConnection): TimerArbeidetRepositoryPostgres {
            return TimerArbeidetRepositoryPostgres(connection)
        }
    }
}