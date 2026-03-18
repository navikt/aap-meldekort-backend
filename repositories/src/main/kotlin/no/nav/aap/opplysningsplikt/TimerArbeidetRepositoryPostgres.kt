package no.nav.aap.opplysningsplikt

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.utfylling.UtfyllingReferanse
import java.time.LocalDate
import no.nav.aap.komponenter.type.Periode as DBPeriode

class TimerArbeidetRepositoryPostgres(
    private val connection: DBConnection,
) : TimerArbeidetRepository{

    override fun lagreTimerArbeidet(ident: Ident, opplysninger: List<TimerArbeidet>) {
        connection.executeBatch(
            """
                insert into timer_arbeidet
                (ident, registreringstidspunkt, utfylling_referanse, fagsak_system, fagsak_nummer, dato, timer_arbeidet)
                values (?, ?, ?, ?, ?, ?, ?)
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
