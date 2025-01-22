package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.Periode
import java.time.LocalDate

data class InnsendingPayload(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    val timerArbeidet: List<TimerArbeidet>,
    val stemmerOpplysningene: Boolean?,
) {
    companion object {
        fun tomtSkjema(meldeperiode: Periode): InnsendingPayload {
            return InnsendingPayload(
                svarerDuSant = null,
                harDuJobbet = null,
                timerArbeidet = meldeperiode.map { TimerArbeidet(null, it) },
                stemmerOpplysningene = null
            )
        }
    }
}

data class TimerArbeidet(
    val timer: Double?,
    val dato: LocalDate,
)

class ArenaSkjemaFlate(
    private val meldekortService: MeldekortService,
    private val utfyllingService: UtfyllingService,
    private val skjemaService: SkjemaService,
    private val arenaClient: ArenaClient
) {
    fun korrigerMeldekort(innloggetBruker: InnloggetBruker, originalMeldekortId: Long, timerArbeidet: List<TimerArbeidet>) {
        skjemaService.sendInnKorrigering(innloggetBruker, originalMeldekortId, timerArbeidet)
    }

    fun kommendeMeldekort(innloggetBruker: InnloggetBruker): List<KommendeMeldekort> {
        return requireNotNull(meldekortService.kommendeMeldekort(innloggetBruker))
    }

    data class HistoriskMeldekortDetaljer(
        val meldekort: HistoriskMeldekort,
        val timerArbeidet: List<TimerArbeidet>?
    )

    fun historiskMeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): HistoriskMeldekortDetaljer {
        val meldekort = meldekortService.historiskeMeldekort(innloggetBruker).single { it.meldekortId == meldekortId }
        return HistoriskMeldekortDetaljer(
            meldekort = meldekort,
            timerArbeidet = arenaClient.meldekortdetaljer(innloggetBruker, meldekortId)
                .timerArbeidet(meldekort.periode.fom)
        )

    }

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker): List<HistoriskMeldekort> {
        return meldekortService.historiskeMeldekort(innloggetBruker).groupBy { it.periode }.values.map {
            it.maxBy { meldekort -> meldekort.beregningStatus.ordinal }
        }
    }

    fun hentEllerOpprettUtfylling(innloggetBruker: InnloggetBruker, meldekortId: Long): Utfylling {
        return utfyllingService.hentEllerStartUtfylling(meldekortId, innloggetBruker)
    }

    fun gåTilNesteSteg(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long,
        fraSteg: StegNavn,
        nyPayload: InnsendingPayload
    ): Utfylling {
        val utfylling = utfyllingService.hentUtfylling(
            ident = innloggetBruker.ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(fraSteg)
            ?.nyPayload(nyPayload)
            ?: throw Error() /* todo: 404 not found */

        utfylling.validerUtkast()

        try {
            return utfyllingService.lagreOgNeste(
                innloggetBruker = innloggetBruker,
                utfylling = utfylling,
            )
        } catch (e: ArenaInnsendingFeiletException) {
            throw e.copy(skjema = utfylling)
        }
    }

    fun lagreSteg(ident: Ident, meldekortId: Long, nyPayload: InnsendingPayload, settSteg: StegNavn): Utfylling {
        val utfylling = utfyllingService.hentUtfylling(
            ident = ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(settSteg)
            ?.nyPayload(nyPayload)
            ?: throw Error() /* todo: 404 not found */

        utfylling.validerUtkast()
        return utfyllingService.lagre(utfylling)
    }
}