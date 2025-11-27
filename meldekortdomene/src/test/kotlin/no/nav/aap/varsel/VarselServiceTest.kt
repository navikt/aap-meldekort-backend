package no.nav.aap.varsel

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinMottakService
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.kelvin.KelvinSakService
import no.nav.aap.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.meldekort.fødselsnummerGenerator
import no.nav.aap.meldekort.saksnummerGenerator
import no.nav.aap.opplysningsplikt.TimerArbeidetRepositoryPostgres
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import no.nav.aap.utfylling.UtfyllingStegNavn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.test.Test

class VarselServiceTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            System.setProperty("aap.meldekort.lenke", "test")
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        }
    }

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        every { varselGateway.sendVarsel(any(), any(), any(), any()) } just Runs
        every { varselGateway.inaktiverVarsel(any()) } just Runs
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
        dataSource = TestDataSource()
    }

    private val zoneId = ZoneId.systemDefault()

    private fun clockMedTid(dateTime: LocalDateTime): Clock {
        return Clock.fixed(
            Instant.from(dateTime.atZone(zoneId).toInstant()),
            zoneId
        )
    }


    @Test
    fun `planlegger varsler, og ingen endring i varsler dersom ny identisk info i mottak`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27),
                LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 10)
            )
            val meldeplikt = lagPerioder(
                LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4),
                LocalDate.of(2025, 8, 11) to LocalDate.of(2025, 8, 18)
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 8, 11, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 28), LocalDate.of(2025, 8, 10)),
                    status = VarselStatus.PLANLAGT
                )
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 7, 28).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 8, 11, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 28), LocalDate.of(2025, 8, 10)),
                    status = VarselStatus.PLANLAGT
                )
            )
        }
    }

    @Test
    fun `sender varsel tidligere (17 desember) for meldeperioden i uke 50-51`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 11, 29), LocalDate.of(2026, 6, 30))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = listOf(Periode(LocalDate.of(2025, 12, 8), LocalDate.of(2025, 12, 21)))

            val meldeplikt = listOf(Periode(LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 29)))

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 12, 17).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 12, 17, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 12, 8), LocalDate.of(2025, 12, 21)),
                    status = VarselStatus.PLANLAGT
                )
            )

            // Sender planlagte varsler
            varselService(connection, clockMedTid(LocalDateTime.of(2025, 12, 17, 9, 0)))
                .sendPlanlagteVarsler()

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 12, 22).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            // Ikke planlagt noen flere varsler
            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 12, 17, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 12, 8), LocalDate.of(2025, 12, 21)),
                    status = VarselStatus.SENDT
                ),
            )
        }
    }

    @Test
    fun `lager ikke nytt varsel for sendt varsel i nåværende meldevindu`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27),
            )
            val meldeplikt = lagPerioder(
                LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4),
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                ),
            )

            varselService(connection, clockMedTid(LocalDateTime.of(2025, 7, 28, 9, 0)))
                .sendPlanlagteVarsler()

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 7, 28).atTime(9, 10))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt,
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.SENDT
                )
            )
        }
    }


    @Test
    fun `ved ny info fra mottak der det mangler en meldepliktperiode det er sendt varsel for, inaktivers varselet, planlagte varsler som mangler slettes`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27),
                LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 10)
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4),
                    LocalDate.of(2025, 8, 11) to LocalDate.of(2025, 8, 18)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 8, 11, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 28), LocalDate.of(2025, 8, 10)),
                    status = VarselStatus.PLANLAGT
                )
            )

            varselService(connection, clockMedTid(LocalDateTime.of(2025, 7, 14, 10, 0)))
                .sendPlanlagteVarsler()

            verify(exactly = 1) { varselGateway.sendVarsel(ident, any(), any(), any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 8, 11, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 28), LocalDate.of(2025, 8, 10)),
                    status = VarselStatus.PLANLAGT
                )
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 7, 27).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            verify(exactly = 1) { varselGateway.inaktiverVarsel(any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.INAKTIVERT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )
        }
    }

    @Test
    fun `ved ny info fra mottak der det er en tidligere meldepliktperiode det er sendt varsel for, så beholdes varselet`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27)
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            varselService(connection, clockMedTid(LocalDateTime.of(2025, 7, 14, 10, 0)))
                .sendPlanlagteVarsler()

            verify(exactly = 1) { varselGateway.sendVarsel(ident, any(), any(), any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 7, 27).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            verify(exactly = 0) { varselGateway.inaktiverVarsel(any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )
        }

    }


    @Test
    fun `lager ingen varsler dersom meldeplikt er tom`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = lagPerioder(
                    LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 10)
                ),
                meldeplikt = emptyList(),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(varselRepository, saksnummer)
        }
    }

    @Test
    fun `lager ikke varsel for meldeplikt der det allerede er en ferdig utfylling`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)

            val utfyllingRepository = UtfyllingRepositoryPostgres(connection)

            utfyllingRepository.lagrUtfylling(
                byggUtfylling(
                    saksnummer = saksnummer,
                    ident = ident,
                    periode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    UtfyllingFlytNavn.AAP_FLYT.steg.last()
                )
            )

            utfyllingRepository.lagrUtfylling(
                byggUtfylling(
                    saksnummer = saksnummer,
                    ident = ident,
                    periode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    UtfyllingFlytNavn.AAP_FLYT.steg.first()
                )
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = lagPerioder(
                    LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27)
                ),
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )

        }
    }


    @Test
    fun `inaktiverer varsel når det mottas en utfylling for perioden varselet er sendt`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 16), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = lagPerioder(
                LocalDate.of(2025, 6, 16) to LocalDate.of(2025, 6, 27),
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 6, 30).plusYears(1)
            )
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 6, 16) to LocalDate.of(2025, 6, 29),
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27)
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 7),
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            varselService(connection, clockMedTid(LocalDateTime.of(2025, 6, 30, 10, 0)))
                .sendPlanlagteVarsler()
            varselService(connection, clockMedTid(LocalDateTime.of(2025, 7, 14, 10, 0)))
                .sendPlanlagteVarsler()

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 6, 30, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 16), LocalDate.of(2025, 6, 27)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )

            varselService(
                connection,
                clockMedTid(LocalDate.of(2025, 7, 15).atTime(1, 1))
            ).inaktiverVarslerForUtfylling(
                byggUtfylling(
                    saksnummer = saksnummer,
                    ident = ident,
                    periode = Periode(LocalDate.of(2025, 6, 16), LocalDate.of(2025, 6, 29)),
                    UtfyllingFlytNavn.AAP_FLYT.steg.last()
                )
            )

            verify(exactly = 1) { varselGateway.inaktiverVarsel(any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 6, 30, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 16), LocalDate.of(2025, 6, 27)),
                    status = VarselStatus.INAKTIVERT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.SENDT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )
        }
    }

    @Test
    fun `avbryter sending og sletter planlagt varsel som skal sendes dersom det allerede er en utfylling for perioden`() {
        dataSource.transaction { connection ->
            val varselRepository = VarselRepositoryPostgres(connection)
            val utfyllingRepository = UtfyllingRepositoryPostgres(connection)

            val saksnummer = saksnummerGenerator.next()
            val ident = fødselsnummerGenerator.next()
            val sakenGjelderFor = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 30).plusYears(1))
            val opplysningsbehov = listOf(sakenGjelderFor)
            val meldeperioder = lagPerioder(
                LocalDate.of(2025, 6, 30) to LocalDate.of(2025, 7, 13),
                LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 27)
            )

            kelvinMottakService(
                connection,
                clockMedTid(LocalDate.of(2025, 6, 30).atTime(1, 1))
            ).behandleMottatteMeldeperioder(
                saksnummer,
                sakenGjelderFor,
                listOf(ident),
                meldeperioder = meldeperioder,
                meldeplikt = lagPerioder(
                    LocalDate.of(2025, 7, 14) to LocalDate.of(2025, 7, 21),
                    LocalDate.of(2025, 7, 28) to LocalDate.of(2025, 8, 4)
                ),
                opplysningsbehov,
                KelvinSakStatus.LØPENDE
            )

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 14, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    status = VarselStatus.PLANLAGT
                ),
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )

            utfyllingRepository.lagrUtfylling(
                byggUtfylling(
                    saksnummer = saksnummer,
                    ident = ident,
                    periode = Periode(LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 13)),
                    UtfyllingFlytNavn.AAP_FLYT.steg.last()
                )
            )

            varselService(connection, clockMedTid(LocalDateTime.of(2025, 7, 14, 10, 0)))
                .sendPlanlagteVarsler()

            verify(exactly = 0) { varselGateway.sendVarsel(ident, any(), any(), any()) }

            assertVarsler(
                varselRepository, saksnummer,
                ForventetVarsel(
                    sendingstidspunkt = LocalDateTime.of(2025, 7, 28, 9, 0),
                    forPeriode = Periode(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 7, 27)),
                    status = VarselStatus.PLANLAGT
                )
            )
        }
    }

    private fun lagPerioder(vararg fomTom: Pair<LocalDate, LocalDate>): List<Periode> {
        return fomTom.map { (fom, tom) -> Periode(fom, tom) }
    }

    private data class ForventetVarsel(
        val sendingstidspunkt: LocalDateTime,
        val forPeriode: Periode,
        val status: VarselStatus
    )

    private fun assertVarsler(
        varselRepository: VarselRepository,
        saksnummer: Fagsaknummer,
        vararg forventedeVarsler: ForventetVarsel
    ) {
        val varsler = varselRepository.hentVarsler(saksnummer)
        assertThat(varsler).hasSize(forventedeVarsler.size)
        forventedeVarsler.forEach { forventetVarsel ->
            assertThat(varsler).anySatisfy { varsel ->
                assertPlanlagtVarselOmMeldeplikt(
                    saksnummer = saksnummer,
                    varsel = varsel,
                    forventetVarsel = forventetVarsel
                )
            }
        }
    }

    private fun assertPlanlagtVarselOmMeldeplikt(
        saksnummer: Fagsaknummer,
        varsel: Varsel,
        forventetVarsel: ForventetVarsel
    ) {
        assertThat(varsel.typeVarsel).isEqualTo(TypeVarsel.OPPGAVE)
        assertThat(varsel.typeVarselOm).isEqualTo(TypeVarselOm.MELDEPLIKTPERIODE)
        assertThat(varsel.saksnummer).isEqualTo(saksnummer)
        assertThat(varsel.sendingstidspunkt.atZone(zoneId).toLocalDateTime()).isEqualTo(
            forventetVarsel.sendingstidspunkt
        )
        assertThat(varsel.status).isEqualTo(forventetVarsel.status)
        assertThat(varsel.forPeriode.fom).isEqualTo(forventetVarsel.forPeriode.fom)
        assertThat(varsel.forPeriode.tom).isEqualTo(forventetVarsel.forPeriode.tom)
    }

    private fun kelvinMottakService(connection: DBConnection, clock: Clock): KelvinMottakService {
        return KelvinMottakService(
            varselService(connection, clock), KelvinSakRepositoryPostgres(connection)
        )
    }

    private val varselGateway = mockk<VarselGateway>()
    private fun varselService(connection: DBConnection, clock: Clock): VarselService {
        val timerArbeidetRepository = TimerArbeidetRepositoryPostgres(connection)
        val kelvinSakRepository = KelvinSakRepositoryPostgres(connection)
        return VarselService(
            kelvinSakService = KelvinSakService(
                kelvinSakRepository = kelvinSakRepository,
                timerArbeidetRepository = timerArbeidetRepository,
                clock = clock
            ),
            kelvinSakRepository = kelvinSakRepository,
            varselRepository = VarselRepositoryPostgres(connection),
            utfyllingRepository = UtfyllingRepositoryPostgres(connection),
            varselGateway = varselGateway,
            clock = clock
        )
    }

    private fun byggUtfylling(
        saksnummer: Fagsaknummer,
        ident: Ident,
        periode: Periode,
        aktivtSteg: UtfyllingStegNavn
    ): Utfylling {
        return Utfylling(
            referanse = UtfyllingReferanse(UUID.randomUUID()),
            fagsak = FagsakReferanse(FagsystemNavn.KELVIN, saksnummer),
            ident = ident,
            periode = periode,
            flyt = UtfyllingFlytNavn.AAP_FLYT,
            aktivtSteg = aktivtSteg,
            svar = Svar.tomt(periode),
            opprettet = Instant.now(),
            sistEndret = Instant.now(),
        )

    }
}
