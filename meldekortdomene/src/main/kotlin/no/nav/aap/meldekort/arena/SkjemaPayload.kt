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
    val meldekortService: MeldekortService,
    val skjemaService: SkjemaService,
    val arenaClient: ArenaClient
) {
    fun listMeldekort(innloggetBruker: InnloggetBruker): List<Meldekort>? {
        return meldekortService.alleMeldekort(innloggetBruker)
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

    fun hentEllerOpprettSkjema(innloggetBruker: InnloggetBruker, meldekortId: Long): Skjema {
        return skjemaService.hentEllerOpprettSkjema(meldekortId, innloggetBruker)
    }

    fun gåTilNesteSteg(
        innloggetBruker: InnloggetBruker,
        meldekortId: Long,
        fraSteg: StegNavn,
        nyPayload: InnsendingPayload
    ): Skjema {
        val skjema = skjemaService.hentSkjema(
            ident = innloggetBruker.ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(fraSteg)
            ?.copy(payload = nyPayload)
            ?: throw Error() /* todo: 404 not found */

        skjema.validerUtkast()

        try {
            return skjemaService.lagreOgNeste(
                innloggetBruker = innloggetBruker,
                skjema = skjema,
            )
        } catch (e: ArenaInnsendingFeiletException) {
            throw e.copy(skjema = skjema)
        }
    }

    fun lagreSteg(ident: Ident, meldekortId: Long, nyPayload: InnsendingPayload, settSteg: StegNavn): Skjema {
        val skjema = skjemaService.hentSkjema(
            ident = ident,
            meldekortId = meldekortId,
        )
            ?.medSteg(settSteg)
            ?.copy(payload = nyPayload)
            ?: throw Error() /* todo: 404 not found */

        skjema.validerUtkast()
        return skjemaService.lagre(skjema)
    }
}