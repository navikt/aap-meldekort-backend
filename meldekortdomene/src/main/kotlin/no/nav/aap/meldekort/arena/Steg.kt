package no.nav.aap.meldekort.arena

enum class StegNavn(val steg: Steg) {
    BEKREFT_SVARER_ÆRLIG(BekreftSvarerÆrlig),
    JOBBET_I_MELDEPERIODEN(JobbetIMeldeperioden),
    TIMER_ARBEIDET(TimerArbeidet),
    KVITTERING(Kvittering)
}

interface Steg {
    val navn: StegNavn
    fun nesteSteg(meldekortskjema: Meldekortskjema): Steg?
}

object BekreftSvarerÆrlig: Steg {
    override val navn: StegNavn
        get() = StegNavn.BEKREFT_SVARER_ÆRLIG

    override fun nesteSteg(meldekortskjema: Meldekortskjema): Steg? {
        return when (meldekortskjema.svarerDuSant) {
            true -> JobbetIMeldeperioden
            false -> Kvittering
            null -> null
        }
    }
}

object JobbetIMeldeperioden: Steg {
    override val navn: StegNavn
        get() = StegNavn.JOBBET_I_MELDEPERIODEN

    override fun nesteSteg(meldekortskjema: Meldekortskjema): Steg? {
        return when (meldekortskjema.harDuJobbet) {
            null -> null
            else -> TimerArbeidet
        }
    }
}

object TimerArbeidet: Steg {
    override val navn: StegNavn
        get() = StegNavn.TIMER_ARBEIDET

    override fun nesteSteg(meldekortskjema: Meldekortskjema): Steg? {
        return when (meldekortskjema.stemmerOpplysningene) {
            true -> Kvittering
            false -> null
            null -> null
        }
    }
}

object Kvittering: Steg {
    override val navn: StegNavn
        get() = StegNavn.KVITTERING

    override fun nesteSteg(meldekortskjema: Meldekortskjema): Steg? {
        return null
    }
}
