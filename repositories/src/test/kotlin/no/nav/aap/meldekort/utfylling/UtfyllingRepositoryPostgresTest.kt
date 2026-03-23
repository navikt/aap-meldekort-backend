package no.nav.aap.meldekort.utfylling

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.meldekort.fødselsnummerGenerator
import no.nav.aap.meldekort.saksnummerGenerator
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.FraværSvar
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.AktivitetsInformasjon
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class UtfyllingRepositoryPostgresTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
        dataSource = TestDataSource()
    }

    @Test
    fun `enkel read write`() {
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)


            val flyt = UtfyllingFlytNavn.AAP_FLYT
            val ident = randomIdent()
            val opprettet = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val referanse = UtfyllingReferanse.ny()
            val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 13))

            val fagsak = FagsakReferanse(
                system = FagsystemNavn.KELVIN,
                nummer = randomFagsaknummer(),
            )

            stubSakOgPerson(
                connection,
                fagsak,
                listOf(ident),
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
            )

            val utfyllingInn1 = Utfylling(
                referanse = referanse,
                ident = ident,
                periode = periode,
                flyt = flyt,
                aktivtSteg = flyt.steg.first(),
                svar = Svar(
                    svarerDuSant = null,
                    harDuJobbet = true,
                    aktivitetsInformasjon = listOf(
                        AktivitetsInformasjon(LocalDate.of(2020, 1, 1), null, null),
                        AktivitetsInformasjon(LocalDate.of(2020, 1, 2), 0.0, null),
                        AktivitetsInformasjon(LocalDate.of(2020, 1, 3), 7.0, null),
                        AktivitetsInformasjon(LocalDate.of(2020, 1, 4), 7.5, null),
                    ),
                    stemmerOpplysningene = false,
                    harDuGjennomførtAvtaltAktivitet = FraværSvar.GJENNOMFØRT_AVTALT_AKTIVITET,
                ),
                opprettet = opprettet,
                sistEndret = opprettet,
                fagsak = fagsak,
            )

            repo.lagreUtfylling(utfyllingInn1)

            repo.lastÅpenUtfylling(ident, periode).also {
                assertEquals(utfyllingInn1, it)
            }

            repo.lastUtfylling(ident, referanse).also {
                assertEquals(utfyllingInn1, it)
            }

            repo.lastUtfylling(ident, periode).also {
                assertEquals(utfyllingInn1, it)
            }

            repo.lastAvsluttetUtfylling(ident, referanse).also {
                assertNull(it)
            }

            val utfyllingInn2 = utfyllingInn1.copy(
                sistEndret = opprettet.plusMillis(100),
                svar = utfyllingInn1.svar.copy(harDuJobbet = false)
            )
            repo.lagreUtfylling(utfyllingInn2)

            repo.lastÅpenUtfylling(ident, periode).also {
                assertEquals(utfyllingInn2, it)
            }

            repo.lastUtfylling(ident, referanse).also {
                assertEquals(utfyllingInn2, it)
            }

            repo.lastUtfylling(ident, periode).also {
                assertEquals(utfyllingInn2, it)
            }

            repo.lastAvsluttetUtfylling(ident, referanse).also {
                assertNull(it)
            }

            val endeligUtfylling = utfyllingInn2.copy(
                sistEndret = utfyllingInn2.sistEndret.plusMillis(300),
                aktivtSteg = utfyllingInn2.flyt.steg.last(),
            )
            assertTrue(endeligUtfylling.erAvsluttet)
            repo.lagreUtfylling(endeligUtfylling)

            repo.lastÅpenUtfylling(ident, periode).also {
                assertNull(it)
            }

            repo.lastUtfylling(ident, referanse).also {
                assertEquals(endeligUtfylling, it)
            }

            repo.lastUtfylling(ident, periode).also {
                assertEquals(endeligUtfylling, it)
            }

            repo.lastAvsluttetUtfylling(ident, referanse).also {
                assertEquals(endeligUtfylling, it)
            }
        }
    }

    @Test
    fun `kan hente utfyllinger med alle identer til en person`() {
        val person1Identer = listOf(randomIdent(), randomIdent())
        val (person1Ident1, person1Ident2) = person1Identer
        val person2Ident = randomIdent()

        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val fagsak = FagsakReferanse(
                system = FagsystemNavn.KELVIN,
                nummer = randomFagsaknummer(),
            )

            stubSakOgPerson(
                connection,
                fagsak,
                person1Identer,
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
            )

            val utfyllingPeriode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 13))
            val utfyllingReferanse = repo.ny(
                saksnummer = fagsak.nummer,
                ident = person1Ident1,
                opprettet = LocalDate.of(2020, 1, 1),
                periode = utfyllingPeriode
            )

            repo.lastÅpenUtfylling(person1Ident1, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastÅpenUtfylling(person1Ident2, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastÅpenUtfylling(person2Ident, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isNull()
            }

            repo.lastUtfylling(person1Ident1, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastUtfylling(person1Ident2, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastUtfylling(person2Ident, utfyllingPeriode).also { utfylling ->
                assertThat(utfylling?.referanse).isNull()
            }

            repo.lastUtfylling(person1Ident1, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastUtfylling(person1Ident2, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastUtfylling(person2Ident, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isNull()
            }

            val utfylling = repo.lastUtfylling(person1Ident1, utfyllingReferanse)!!
            val avsluttetUtfylling = utfylling.copy(
                aktivtSteg = utfylling.flyt.steg.last(),
            )
            assertTrue(avsluttetUtfylling.erAvsluttet)
            repo.lagreUtfylling(avsluttetUtfylling)

            repo.lastAvsluttetUtfylling(person1Ident1, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastAvsluttetUtfylling(person1Ident2, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isEqualTo(utfyllingReferanse)
            }
            repo.lastAvsluttetUtfylling(person2Ident, utfyllingReferanse).also { utfylling ->
                assertThat(utfylling?.referanse).isNull()
            }
        }
    }

    @Test
    fun `slett gamle utkast`() {
        val ident = randomIdent()
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val ref = repo.ny(
                saksnummer = randomFagsaknummer(),
                ident = ident,
                opprettet = LocalDate.of(2020, 1, 1),
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
            )
            repo.slettGamleUtkast(LocalDate.of(2020, 1, 1))

            assertNull(repo.lastUtfylling(ident, ref))
        }
    }

    @Test
    fun `slett enda eldre utkast`() {
        val ident = randomIdent()
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val ref = repo.ny(
                saksnummer = randomFagsaknummer(),
                ident = ident,
                opprettet = LocalDate.of(2019, 1, 1),
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
            )
            repo.slettGamleUtkast(LocalDate.of(2020, 1, 1))

            assertNull(repo.lastUtfylling(ident, ref))
        }
    }

    @Test
    fun `ikke slett gamle innsendte`() {
        val ident = randomIdent()
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val fagsak = FagsakReferanse(
                system = FagsystemNavn.KELVIN,
                nummer = randomFagsaknummer(),
            )
            stubSakOgPerson(
                connection,
                fagsak,
                listOf(ident),
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
            )

            val utfylling =
                repo.ny(
                    saksnummer = fagsak.nummer,
                    ident = ident,
                    opprettet = LocalDate.of(2019, 1, 1),
                    periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
                ).let {
                    repo.lastUtfylling(ident, it)!!
                }

            repo.lagreUtfylling(
                utfylling.copy(
                    aktivtSteg = utfylling.flyt.steg.last(),
                )
            )
            repo.slettGamleUtkast(LocalDate.of(2020, 1, 1))

            assertNotNull(repo.lastUtfylling(ident, utfylling.referanse))
        }
    }

    @Test
    fun `ikke slett nyere`() {
        val ident = randomIdent()
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)
            val fagsak = FagsakReferanse(
                system = FagsystemNavn.KELVIN,
                nummer = randomFagsaknummer(),
            )
            stubSakOgPerson(
                connection,
                fagsak,
                listOf(ident),
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
            )

            val ref = repo.ny(
                saksnummer = fagsak.nummer,
                ident = ident,
                opprettet = LocalDate.of(2020, 1, 3),
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
            )
            repo.slettGamleUtkast(LocalDate.of(2020, 1, 1))

            assertNotNull(repo.lastUtfylling(ident, ref))
        }
    }

    @Test
    fun `hent utfyllinger for sak`() {
        dataSource.transaction { connection ->
            val repo = UtfyllingRepositoryPostgres(connection)

            val sak1 = saksnummerGenerator.next()
            val sak2 = saksnummerGenerator.next()

            repo.ny(
                saksnummer = sak1,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 6, 16),
                periode = Periode(LocalDate.of(2025, 6, 12), LocalDate.of(2025, 6, 25))
            )
            repo.ny(
                saksnummer = sak1,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 6, 27),
                periode = Periode(LocalDate.of(2025, 6, 26), LocalDate.of(2025, 7, 8))
            )
            repo.ny(
                saksnummer = sak1,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 5, 6),
                periode = Periode(LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 18))
            )
            repo.ny(
                saksnummer = sak1,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 5, 24),
                periode = Periode(LocalDate.of(2025, 5, 19), LocalDate.of(2025, 6, 1))
            )
            repo.ny(
                saksnummer = sak2,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 4, 9),
                periode = Periode(LocalDate.of(2025, 4, 7), LocalDate.of(2025, 4, 20))
            )
            repo.ny(
                saksnummer = sak2,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 4, 21),
                periode = Periode(LocalDate.of(2025, 4, 21), LocalDate.of(2025, 5, 4))
            )
            repo.ny(
                saksnummer = sak2,
                ident = fødselsnummerGenerator.next(),
                opprettet = LocalDate.of(2025, 5, 7),
                periode = Periode(LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 18))
            )

            assertThat(repo.hentUtfyllinger(sak1).map { it.periode }).containsExactlyInAnyOrder(
                Periode(LocalDate.of(2025, 6, 12), LocalDate.of(2025, 6, 25)),
                Periode(LocalDate.of(2025, 6, 26), LocalDate.of(2025, 7, 8)),
                Periode(LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 18)),
                Periode(LocalDate.of(2025, 5, 19), LocalDate.of(2025, 6, 1))
            )

            assertThat(repo.hentUtfyllinger(sak2).map { it.periode }).containsExactlyInAnyOrder(
                Periode(LocalDate.of(2025, 4, 7), LocalDate.of(2025, 4, 20)),
                Periode(LocalDate.of(2025, 4, 21), LocalDate.of(2025, 5, 4)),
                Periode(LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 18))
            )
        }
    }

    fun stubSakOgPerson(connection: DBConnection, fagsak: FagsakReferanse, identer: List<Ident>, periode: Periode) {
        val repo = KelvinSakRepositoryPostgres(connection)
        val sak = repo.upsertSak(
            saksnummer = fagsak.nummer,
            sakenGjelderFor = periode,
            identer = identer,
            meldeperioder = emptyList(),
            meldeplikt = emptyList(),
            opplysningsbehov = emptyList(),
            status = null,
        )
    }
}

private fun UtfyllingRepositoryPostgres.ny(
    saksnummer: Fagsaknummer,
    ident: Ident,
    opprettet: LocalDate,
    periode: Periode,
): UtfyllingReferanse {
    val utfylingReferanse = UtfyllingReferanse.ny()
    this.lagreUtfylling(
        Utfylling(
            referanse = utfylingReferanse,
            fagsak = FagsakReferanse(FagsystemNavn.KELVIN, saksnummer),
            ident = ident,
            periode = periode,
            flyt = UtfyllingFlytNavn.AAP_KORRIGERING_FLYT,
            aktivtSteg = UtfyllingFlytNavn.AAP_KORRIGERING_FLYT.steg.first(),
            svar = Svar.tomt(periode),
            opprettet = opprettet.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(),
            sistEndret = opprettet.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(),
        )
    )

    return utfylingReferanse
}

fun randomIdent(): Ident {
    return fødselsnummerGenerator.next()
}

fun randomFagsaknummer(): Fagsaknummer {
    return Fagsaknummer(CharArray(10) { ('a'..'z').random() }.concatToString())
}
