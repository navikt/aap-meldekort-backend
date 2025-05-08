package no.nav.aap.meldekort.opplysningsplikt

import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.opplysningsplikt.TimerArbeidet
import no.nav.aap.opplysningsplikt.TimerArbeidetRepositoryPostgres
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.utfylling.UtfyllingReferanse
import org.junit.jupiter.api.Test

class TimerArbeidetRepositoryPostgresTest {
    private val ident1 = Ident("1".repeat(11))
    private val ident2 = Ident("1".repeat(11))
    private val utfylling1 = UtfyllingReferanse.ny()
    private val utfylling2 = UtfyllingReferanse.ny()
    private val fagsak1 = FagsakReferanse(
        system = FagsystemNavn.KELVIN,
        nummer = Fagsaknummer("123"),
    )
    private val fagsak2 = FagsakReferanse(
        system = FagsystemNavn.ARENA,
        nummer = Fagsaknummer("123"),
    )

    @Test
    fun `skriv og les timer arbeidet`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val t0 = Instant.ofEpochSecond(1739960455)
            val t1 = t0.plusSeconds(1)

            val repo = TimerArbeidetRepositoryPostgres(connection)

            /* Lagr forskjellige opplysninger på forskjellige fagsaker men med samme saksnummer */
            repo.lagrTimerArbeidet(
                ident = ident1,
                opplysninger = listOf(
                    TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 1), null),
                    TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 2), 0.0),
                    TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 3), 0.5),
                    TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 4), 7.5),
                    TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 5), null),
                )
            )

            repo.lagrTimerArbeidet(
                ident = ident1,
                opplysninger = listOf(
                    TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 1), 1.0),
                    TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 2), null),
                    TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 3), 3.5),
                    TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 4), null),
                    TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 5), 1.0),
                )
            )

            /* Gjør delevis overskriving av periodene */
            repo.lagrTimerArbeidet(
                ident = ident1,
                opplysninger = listOf(
                    TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 4), null),
                    TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 5), 8.0),
                    TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 6), 4.0),
                )
            )
            repo.lagrTimerArbeidet(
                ident = ident1,
                opplysninger = listOf(
                    TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 4), 3.5),
                    TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 5), null),
                    TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 6), 1.0),
                )
            )

            /* Leser ut den nye verdien der det er overlapp, ellers den første verdien. Ingen verdi
            * dersom det ikke er oppgitt noe. */
            val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 7))
            repo.hentTimerArbeidet(ident1, fagsak1, periode).also { effektiveOpplysninger ->
                assertEquals(
                    listOf(
                        TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 1), null),
                        TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 2), 0.0),
                        TimerArbeidet(t0, utfylling1, fagsak1, LocalDate.of(2020, 1, 3), 0.5),
                        TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 4), null),
                        TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 5), 8.0),
                        TimerArbeidet(t1, utfylling1, fagsak1, LocalDate.of(2020, 1, 6), 4.0),
                    ),
                    effektiveOpplysninger
                )
            }

            repo.hentTimerArbeidet(ident1, fagsak2, periode).also { effektiveOpplysninger ->
                assertEquals(
                    listOf(
                        TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 1), 1.0),
                        TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 2), null),
                        TimerArbeidet(t0, utfylling2, fagsak2, LocalDate.of(2020, 1, 3), 3.5),
                        TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 4), 3.5),
                        TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 5), null),
                        TimerArbeidet(t1, utfylling2, fagsak2, LocalDate.of(2020, 1, 6), 1.0),
                    ),
                    effektiveOpplysninger
                )
            }
        }
    }
}