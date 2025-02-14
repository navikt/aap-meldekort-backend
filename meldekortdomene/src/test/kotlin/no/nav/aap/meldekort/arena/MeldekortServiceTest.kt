package no.nav.aap.meldekort.arena

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.arena.ArenaGateway.KortType.ELEKTRONISK
import no.nav.aap.arena.ArenaGateway.KortType.MANUELL_ARENA
import no.nav.aap.arena.ArenaMeldekort.ArenaStatus.OPPRE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortRepositoryFake
import no.nav.aap.arena.MeldekortService

class MeldekortServiceTest {
    val innloggetBruker = InnloggetBruker(
        ident = Ident("9".repeat(11)),
        token = "fake.jwt.token",
    )

    fun meldekortService(
        kommendeMeldekort: List<ArenaMeldekort>?,
        historiskeMeldekort: List<ArenaMeldekort>?
    ): MeldekortService {
        return MeldekortService(
            arenaGateway = ArenaGatewayFake(
                kommendeMeldekort = kommendeMeldekort?.toMutableList(),
                historiskeMeldekort = historiskeMeldekort?.toMutableList(),
            ),
            meldekortRepository = MeldekortRepositoryFake(),
        )
    }

    @Test
    fun `bruker finnes ikke i arena, da krasjer ikke alleMeldekort()`() {
        val meldekortService = meldekortService(
            kommendeMeldekort = null,
            historiskeMeldekort = null,
        )
        val meldekort = meldekortService.alleMeldekort(innloggetBruker)
        assertNull(meldekort)
    }

    @Test
    fun `meldekort er sorted på meldeperiode`() {
        val meldekortService = meldekortService(
            kommendeMeldekort = kommendeMeldekort,
            historiskeMeldekort = listOf(),
        )
        val meldekort = meldekortService.alleMeldekort(innloggetBruker)

        assertThat(meldekort).isSortedAccordingTo { x, y -> x.periode.fom.compareTo(y.periode.fom) }
    }

    val kommendeMeldekort = listOf(
        ArenaMeldekort(
            meldekortId = MeldekortId(1684229691),
            kortType = MANUELL_ARENA,
            meldeperiode = "202444",
            fraDato = LocalDate.parse("2024-10-28"),
            tilDato = LocalDate.parse("2024-11-10"),
            hoyesteMeldegruppe = "ATTF",
            beregningstatus = OPPRE,
            forskudd = false,
            mottattDato = null,
            bruttoBelop = 0.0,
        ),
        ArenaMeldekort(
            meldekortId = MeldekortId(1684229709),
            kortType = MANUELL_ARENA,
            meldeperiode = "202446",
            fraDato = LocalDate.parse("2024-11-11"),
            tilDato = LocalDate.parse("2024-11-24"),
            hoyesteMeldegruppe = "ATTF",
            beregningstatus = OPPRE,
            forskudd = false,
            mottattDato = null,
            bruttoBelop = 0.0,
        ),
        ArenaMeldekort(
            meldekortId = MeldekortId(1684229717),
            kortType = MANUELL_ARENA,
            meldeperiode = "202448",
            fraDato = LocalDate.parse("2024-11-25"),
            tilDato = LocalDate.parse("2024-12-08"),
            hoyesteMeldegruppe = "ATTF",
            beregningstatus = OPPRE,
            forskudd = false,
            mottattDato = null,
            bruttoBelop = 0.0,
        ),
        ArenaMeldekort(
            meldekortId = MeldekortId(1684229725),
            kortType = ELEKTRONISK,
            meldeperiode = "202450",
            fraDato = LocalDate.parse("2024-12-09"),
            tilDato = LocalDate.parse("2024-12-22"),
            hoyesteMeldegruppe = "ATTF",
            beregningstatus = OPPRE,
            forskudd = false,
            mottattDato = null,
            bruttoBelop = 0.0,
        )
    )
}