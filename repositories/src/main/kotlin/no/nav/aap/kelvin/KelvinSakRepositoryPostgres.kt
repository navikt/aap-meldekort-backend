package no.nav.aap.kelvin

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import java.time.LocalDate

class KelvinSakRepositoryPostgres(private val connection: DBConnection) : KelvinSakRepository {
    override fun upsertSak(
        saksnummer: Fagsaknummer,
        sakenGjelderFor: Periode,
        identer: List<Ident>,
        meldeperioder: List<Periode>
    ) {
        val sakId = connection.queryFirst<Long>(
            """
            insert into kelvin_sak (saksnummer, saken_gjelder_for)
            values (?, ?::daterange)
            on conflict (saksnummer)
            do update set
                oppdatert = current_timestamp(3),
                saken_gjelder_for = excluded.saken_gjelder_for
                returning id
        """
        ) {
            setParams {
                setString(1, saksnummer.asString)
                setPeriode(2, no.nav.aap.komponenter.type.Periode(sakenGjelderFor.fom, sakenGjelderFor.tom))
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

    override fun hentSak(ident: Ident, påDag: LocalDate): Sak? {
        return connection.queryFirstOrNull(
            """
            select kelvin_sak.id, kelvin_sak.saksnummer, kelvin_sak.saken_gjelder_for
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
                Sak(
                    referanse = FagsakReferanse(
                        system = FagsystemNavn.KELVIN,
                        nummer = Fagsaknummer(it.getString("saksnummer")),
                    ),
                    rettighetsperiode = it.getPeriodeOrNull("saken_gjelder_for")?.let { Periode(it.fom, it.tom) }
                        ?: Periode(LocalDate.MIN, LocalDate.MAX)
                )
            }
        }
    }

    companion object : Factory<KelvinSakRepository> {
        override fun konstruer(connection: DBConnection): KelvinSakRepository {
            return KelvinSakRepositoryPostgres(connection)
        }
    }
}