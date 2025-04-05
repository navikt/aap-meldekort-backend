package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.sak.Fagsaknummer
import java.time.LocalDate

class KelvinSakRepositoryPostgres(private val connection: DBConnection) : KelvinSakRepository {
    override fun upsertSak(
        saksnummer: Fagsaknummer,
        sakenGjelderFor: Periode,
        identer: List<Ident>,
        meldeperioder: List<Periode>,
        meldeplikt: List<Periode>,
        opplysningsbehov: List<Periode>,
        status: KelvinSakStatus?,
    ) {
        val sakId = connection.queryFirst<Long>(
            """
            insert into kelvin_sak (saksnummer, saken_gjelder_for, status)
            values (?, ?::daterange, ?)
            on conflict (saksnummer)
            do update set
                oppdatert = current_timestamp(3),
                saken_gjelder_for = excluded.saken_gjelder_for,
                status = excluded.status
                returning id
        """
        ) {
            setParams {
                setString(1, saksnummer.asString)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(sakenGjelderFor.fom, sakenGjelderFor.tom))
                setEnumName(3, status)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        val personId = connection.queryFirst<Long>(
            """
            insert into kelvin_person (sak_id) values (?)
            on conflict (sak_id) do update set
            oppdatert = current_timestamp(3)
            returning id
        """
        ) {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        connection.execute(
            """
            delete from kelvin_person_ident
            where person_id = ?
            and ident <> all (?::text[])
        """
        ) {
            setParams {
                setLong(1, personId)
                setArray(2, identer.map { it.asString })
            }
        }

        connection.executeBatch(
            """
            insert into kelvin_person_ident (person_id, ident)
            values (?, ?)
            on conflict (person_id, ident) do update set 
            oppdatert = current_timestamp(3)
        """,
            identer
        ) {
            setParams { ident ->
                setLong(1, personId)
                setString(2, ident.asString)
            }
        }

        connection.execute(
            """
            delete from kelvin_meldeperiode
            where sak_id = ? and periode <> all (?::daterange[])
            """
        ) {
            setParams {
                setLong(1, sakId)
                setPeriodeArray(2, meldeperioder.map { no.nav.aap.komponenter.type.Periode(it.fom, it.tom) })
            }
        }

        connection.executeBatch(
            """
                insert into kelvin_meldeperiode (sak_id, periode)
                values (?, ?::daterange)
                on conflict (sak_id, periode) do update set
                oppdatert = current_timestamp(3)
            """,
            meldeperioder
        ) {
            setParams {
                setLong(1, sakId)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(it.fom, it.tom))
            }
        }

        connection.execute(
            """
            delete from kelvin_fastsatt_periode
            where sak_id = ? and periode <> all (?::daterange[])
            """
        ) {
            setParams {
                setLong(1, sakId)
                setPeriodeArray(2, meldeplikt.map { no.nav.aap.komponenter.type.Periode(it.fom, it.tom) })
            }
        }

        connection.executeBatch(
            """
                insert into kelvin_fastsatt_periode (sak_id, periode)
                values (?, ?::daterange)
                on conflict (sak_id, periode) do update set
                oppdatert = current_timestamp(3)
            """,
            meldeplikt
        ) {
            setParams {
                setLong(1, sakId)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(it.fom, it.tom))
            }
        }

        connection.execute(
            """
            delete from kelvin_opplysningsbehov
            where sak_id = ? and periode <> all (?::daterange[])
            """
        ) {
            setParams {
                setLong(1, sakId)
                setPeriodeArray(2, opplysningsbehov.map { no.nav.aap.komponenter.type.Periode(it.fom, it.tom) })
            }
        }

        connection.executeBatch(
            """
                insert into kelvin_opplysningsbehov (sak_id, periode)
                values (?, ?::daterange)
                on conflict (sak_id, periode) do update set
                oppdatert = current_timestamp(3)
            """,
            opplysningsbehov
        ) {
            setParams {
                setLong(1, sakId)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(it.fom, it.tom))
            }
        }
    }

    override fun hentMeldeperioder(ident: Ident, saksnummer: Fagsaknummer): List<Periode> {
        return connection.queryList(
            """
            select kelvin_meldeperiode.periode from kelvin_meldeperiode
            join kelvin_sak on kelvin_meldeperiode.sak_id = kelvin_sak.id
            join kelvin_person on kelvin_sak.id = kelvin_person.sak_id
            join kelvin_person_ident on kelvin_person.id = kelvin_person_ident.person_id
            where kelvin_person_ident.ident = ? and kelvin_sak.saksnummer = ?
            order by kelvin_meldeperiode.periode
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setString(2, saksnummer.asString)
            }
            setRowMapper {
                it.getPeriode("periode").let { Periode(it.fom, it.tom) }
            }
        }
    }

    override fun hentMeldeplikt(ident: Ident, saksnummer: Fagsaknummer): List<Periode> {
        return connection.queryList(
            """
            select kelvin_fastsatt_periode.periode from kelvin_fastsatt_periode
            join kelvin_sak on kelvin_fastsatt_periode.sak_id = kelvin_sak.id
            join kelvin_person on kelvin_sak.id = kelvin_person.sak_id
            join kelvin_person_ident on kelvin_person.id = kelvin_person_ident.person_id
            where kelvin_person_ident.ident = ? and kelvin_sak.saksnummer = ?
            order by kelvin_fastsatt_periode.periode
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setString(2, saksnummer.asString)
            }
            setRowMapper {
                it.getPeriode("periode").let { Periode(it.fom, it.tom) }
            }
        }
    }

    override fun hentOpplysningsbehov(ident: Ident, saksnummer: Fagsaknummer): List<Periode> {
        return connection.queryList(
            """
            select kelvin_opplysningsbehov.periode from kelvin_opplysningsbehov
            join kelvin_sak on kelvin_opplysningsbehov.sak_id = kelvin_sak.id
            join kelvin_person on kelvin_sak.id = kelvin_person.sak_id
            join kelvin_person_ident on kelvin_person.id = kelvin_person_ident.person_id
            where kelvin_person_ident.ident = ? and kelvin_sak.saksnummer = ?
            order by kelvin_opplysningsbehov.periode
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setString(2, saksnummer.asString)
            }
            setRowMapper {
                it.getPeriode("periode").let { Periode(it.fom, it.tom) }
            }
        }
    }

    override fun hentSak(ident: Ident, påDag: LocalDate): KelvinSak? {
        return connection.queryFirstOrNull(
            """
            select kelvin_sak.id, kelvin_sak.saksnummer, kelvin_sak.saken_gjelder_for, kelvin_sak.status
            from kelvin_sak
            join kelvin_person on kelvin_sak.id = kelvin_person.sak_id
            join kelvin_person_ident on kelvin_person.id = kelvin_person_ident.person_id
            where kelvin_person_ident.ident = ? and kelvin_sak.saken_gjelder_for @> ?::date
        """
        ) {
            setParams {
                setString(1, ident.asString)
                setLocalDate(2, påDag)
            }

            setRowMapper {
                KelvinSak(
                    saksnummer = Fagsaknummer(it.getString("saksnummer")),
                    status = it.getEnumOrNull("status"),
                    rettighetsperiode = it.getPeriodeOrNull("saken_gjelder_for")?.let { Periode(it.fom, it.tom) }
                        ?: Periode(LocalDate.MIN, LocalDate.MAX)
                )
            }
        }
    }

    companion object : RepositoryFactory<KelvinSakRepository> {
        override fun konstruer(connection: DBConnection): KelvinSakRepository {
            return KelvinSakRepositoryPostgres(connection)
        }
    }
}