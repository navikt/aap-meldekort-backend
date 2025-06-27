package no.nav.aap.meldekort.varsel

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepository
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.varsel.TypeVarsel
import no.nav.aap.varsel.TypeVarselOm
import no.nav.aap.varsel.Varsel
import no.nav.aap.varsel.VarselId
import no.nav.aap.varsel.VarselRepositoryPostgres
import no.nav.aap.varsel.VarselStatus
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import java.time.Clock

class VarselRepositoryPostgresTest {
    @Test
    fun `oppretter, henter og oppdaterer varsler`() {
        val saksnummer = Fagsaknummer("123")

        InitTestDatabase.freshDatabase().transaction { connection ->
            val sakRepo = KelvinSakRepositoryPostgres(connection)
            val varselRepo = VarselRepositoryPostgres(connection)

            opprettSak(sakRepo, saksnummer)

            val varsel = byggVarsel(
                saksnummer = saksnummer,
                varselId = VarselId(UUID.randomUUID()),
                typeVarsel = TypeVarsel.OPPGAVE,
                typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                sendingstidspunkt = Instant.now(),
                status = VarselStatus.PLANLAGT,
                forPeriode = Periode(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 22)),
                opprettet = Instant.now(),
                sistEndret = Instant.now()
            )

            varselRepo.upsert(varsel)

            varselRepo.hentVarsler(saksnummer).also { varsler ->
                assertThat(varsler).containsExactly(varsel)
            }

            val oppdatertVarsel = Varsel(
                varselId = varsel.varselId,
                typeVarsel = TypeVarsel.BESKJED,
                typeVarselOm = TypeVarselOm.OPPLYSNINGSBEHOV,
                saksnummer = saksnummer,
                sendingstidspunkt = varsel.sendingstidspunkt.plus(3, ChronoUnit.DAYS),
                status = VarselStatus.SENDT,
                forPeriode = Periode(LocalDate.of(2021, 2, 3), LocalDate.of(2021, 3, 12)),
                opprettet = varsel.opprettet.plus(1, ChronoUnit.DAYS),
                sistEndret = varsel.sistEndret.plus(2, ChronoUnit.DAYS),
            )

            varselRepo.upsert(oppdatertVarsel)

            varselRepo.hentVarsler(saksnummer).also { varsler ->
                assertThat(varsler).containsExactly(oppdatertVarsel)
            }
        }
    }

    @Test
    fun `sletter planlagte varsler`() {
        val saksnummer = Fagsaknummer("1234")

        InitTestDatabase.freshDatabase().transaction { connection ->
            val sakRepo = KelvinSakRepositoryPostgres(connection)
            val varselRepo = VarselRepositoryPostgres(connection)

            opprettSak(sakRepo, saksnummer)
            val planlagtValselMeldepliktperiode = byggVarsel(
                saksnummer = saksnummer,
                status = VarselStatus.PLANLAGT,
                typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE
            )
            val planlagtValselOpplysningsbehov = byggVarsel(
                saksnummer = saksnummer,
                status = VarselStatus.PLANLAGT,
                typeVarselOm = TypeVarselOm.OPPLYSNINGSBEHOV
            )
            val sendtValselMeldepliktperiode = byggVarsel(
                saksnummer = saksnummer,
                status = VarselStatus.SENDT,
                typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE
            )
            val inaktivertValselMeldepliktperiode = byggVarsel(
                saksnummer = saksnummer,
                status = VarselStatus.INAKTIVERT,
                typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE
            )

            varselRepo.upsert(planlagtValselMeldepliktperiode)
            varselRepo.upsert(planlagtValselOpplysningsbehov)
            varselRepo.upsert(sendtValselMeldepliktperiode)
            varselRepo.upsert(inaktivertValselMeldepliktperiode)

            varselRepo.hentVarsler(saksnummer).also { varsler ->
                assertThat(varsler).containsExactlyInAnyOrder(
                    planlagtValselMeldepliktperiode,
                    planlagtValselOpplysningsbehov,
                    sendtValselMeldepliktperiode,
                    inaktivertValselMeldepliktperiode
                )
            }

            varselRepo.slettPlanlagteVarsler(saksnummer, TypeVarselOm.MELDEPLIKTPERIODE)

            varselRepo.hentVarsler(saksnummer).also { varsler ->
                assertThat(varsler).containsExactlyInAnyOrder(
                    planlagtValselOpplysningsbehov,
                    sendtValselMeldepliktperiode,
                    inaktivertValselMeldepliktperiode
                )
            }
        }
    }

    @Test
    fun `henter varsler som skal sendes`() {
        val saksnummer1 = Fagsaknummer("1")
        val saksnummer2 = Fagsaknummer("2")
        val saksnummer3 = Fagsaknummer("3")
        val saksnummer4 = Fagsaknummer("4")

        InitTestDatabase.freshDatabase().transaction { connection ->
            val clock = Clock.systemDefaultZone()
            val sakRepo = KelvinSakRepositoryPostgres(connection)
            val varselRepo = VarselRepositoryPostgres(connection)

            opprettSak(sakRepo, saksnummer1)
            opprettSak(sakRepo, saksnummer2)
            opprettSak(sakRepo, saksnummer3)
            opprettSak(sakRepo, saksnummer4)

            val fortid = Instant.now(clock).minus(1, ChronoUnit.MINUTES)
            val fremtid = Instant.now(clock).plus(1, ChronoUnit.MINUTES)

            val tidligereVarsler = listOf(
                byggVarsel(
                    saksnummer = saksnummer1,
                    typeVarsel = TypeVarsel.BESKJED,
                    typeVarselOm = TypeVarselOm.VALGFRITT_OPPLYSNINGSBEHOV,
                    status = VarselStatus.SENDT,
                    sendingstidspunkt = fortid
                ),
                byggVarsel(
                    saksnummer = saksnummer2,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.OPPLYSNINGSBEHOV,
                    status = VarselStatus.SENDT,
                    sendingstidspunkt = fortid
                ),
                byggVarsel(
                    saksnummer = saksnummer3,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.INAKTIVERT,
                    sendingstidspunkt = fortid
                )
            )

            val varslerSomSkalSendes = listOf(
                byggVarsel(
                    saksnummer = saksnummer1,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fortid
                ),
                byggVarsel(
                    saksnummer = saksnummer2,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fortid
                ),
                byggVarsel(
                    saksnummer = saksnummer3,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fortid
                ),
                byggVarsel(
                    saksnummer = saksnummer4,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.OPPLYSNINGSBEHOV,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fortid
                )
            )
            val fremtidigeVarsler = listOf(
                byggVarsel(
                    saksnummer = saksnummer2,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fremtid
                ),
                byggVarsel(
                    saksnummer = saksnummer3,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fremtid
                ),
                byggVarsel(
                    saksnummer = saksnummer4,
                    typeVarsel = TypeVarsel.OPPGAVE,
                    typeVarselOm = TypeVarselOm.MELDEPLIKTPERIODE,
                    status = VarselStatus.PLANLAGT,
                    sendingstidspunkt = fremtid
                )
            )

            val alleVarsler = tidligereVarsler + varslerSomSkalSendes + fremtidigeVarsler

            alleVarsler.forEach {
                varselRepo.upsert(it)
            }

            assertThat(
                varselRepo.hentVarsler(saksnummer1) +
                        varselRepo.hentVarsler(saksnummer2) +
                        varselRepo.hentVarsler(saksnummer3) +
                        varselRepo.hentVarsler(saksnummer4)
            ).containsExactlyInAnyOrder(
                * alleVarsler.toTypedArray()
            )

            varselRepo.hentVarslerForUtsending(clock).also {
                assertThat(it).containsExactlyInAnyOrder(*varslerSomSkalSendes.toTypedArray())
            }
        }
    }

    private fun opprettSak(sakRepo: KelvinSakRepository, saksnummer: Fagsaknummer) {
        sakRepo.upsertSak(
            saksnummer = saksnummer,
            sakenGjelderFor = Periode(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 22)),
            identer = listOf(Ident("1".repeat(11))),
            meldeperioder = listOf(),
            meldeplikt = listOf(),
            opplysningsbehov = listOf(),
            status = KelvinSakStatus.LØPENDE
        )
    }
}