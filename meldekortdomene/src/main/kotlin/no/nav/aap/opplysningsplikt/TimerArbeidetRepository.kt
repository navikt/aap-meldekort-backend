package no.nav.aap.opplysningsplikt

import no.nav.aap.lookup.repository.Repository
import java.time.Instant
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.sak.Fagsaknummer


interface TimerArbeidetRepository: Repository {
    fun hentTimerArbeidet(ident: Ident, fagsaknummer: Fagsaknummer, periode: Periode): List<TimerArbeidet>
    fun lagrTimerArbeidet(ident: Ident, fagsaknummer: Fagsaknummer, opplysninger: List<TimerArbeidet>)
}

class TimerArbeidetRepositoryFake: TimerArbeidetRepository {
    private class Row(
        val ident: Ident,
        val fagsaknummer: Fagsaknummer,
        val timerArbeidet: TimerArbeidet,
        val tidspunkt: Instant,
        val utkast: Boolean,
    )

    private val opplysninger = mutableListOf<Row>()

    override fun hentTimerArbeidet(
        ident: Ident,
        fagsaknummer: Fagsaknummer,
        periode: Periode,
    ): List<TimerArbeidet> {
        return opplysninger
            .filter { it.ident == ident && it.fagsaknummer == fagsaknummer && it.timerArbeidet.dato in periode && it.utkast == false }
            .groupBy { it.timerArbeidet.dato }
            .map { it.value.maxBy { it.tidspunkt }.timerArbeidet }
    }

    override fun lagrTimerArbeidet(ident: Ident, fagsaknummer: Fagsaknummer, opplysninger: List<TimerArbeidet>) {
        this.opplysninger.addAll(opplysninger.map { Row(ident, fagsaknummer, it, Instant.now(), false) })
    }
}