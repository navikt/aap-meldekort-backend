package no.nav.aap.opplysningsplikt

import no.nav.aap.lookup.repository.Repository
import java.time.Instant
import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.sak.Saksnummer


interface TimerArbeidetRepository: Repository {
    fun hentGjeldendeTimerArbeidet(ident: Ident, saksnummer: Saksnummer, periode: Periode): List<TimerArbeidet>
    fun lagrTimerArbeidet(ident: Ident, saksnummer: Saksnummer, opplysninger: List<TimerArbeidet>)

    fun hentUtkastTimerArbeidet(ident: Ident, saksnummer: Saksnummer, periode: Periode): List<TimerArbeidet>
    fun lagrUtkastTimerArbeidet(ident: Ident, saksnummer: Saksnummer, opplysninger: List<TimerArbeidet>)
    fun slettUtkast(ident: Ident, saksnummer: Saksnummer, periode: Periode? = null)
}

class TimerArbeidetRepositoryFake: TimerArbeidetRepository {
    private class Row(
        val ident: Ident,
        val saksnummer: Saksnummer,
        val timerArbeidet: TimerArbeidet,
        val tidspunkt: Instant,
        val utkast: Boolean,
    )

    private val opplysninger = mutableListOf<Row>()

    override fun hentGjeldendeTimerArbeidet(
        ident: Ident,
        saksnummer: Saksnummer,
        periode: Periode,
    ): List<TimerArbeidet> {
        return opplysninger
            .filter { it.ident == ident && it.saksnummer == saksnummer && it.timerArbeidet.dato in periode && it.utkast == false }
            .groupBy { it.timerArbeidet.dato }
            .map { it.value.maxBy { it.tidspunkt }.timerArbeidet }
    }

    override fun lagrTimerArbeidet(ident: Ident, saksnummer: Saksnummer, opplysninger: List<TimerArbeidet>) {
        this.opplysninger.addAll(opplysninger.map { Row(ident, saksnummer, it, Instant.now(), false) })
    }

    override fun hentUtkastTimerArbeidet(
        ident: Ident,
        saksnummer: Saksnummer,
        periode: Periode,
    ): List<TimerArbeidet> {
        return opplysninger
            .filter { it.ident == ident && it.saksnummer == saksnummer && it.timerArbeidet.dato in periode && it.utkast }
            .groupBy { it.timerArbeidet.dato }
            .map { it.value.maxBy { it.tidspunkt }.timerArbeidet }
    }

    override fun lagrUtkastTimerArbeidet(ident: Ident, saksnummer: Saksnummer, opplysninger: List<TimerArbeidet>) {
        this.opplysninger.addAll(opplysninger.map { Row(ident, saksnummer, it, Instant.now(), true) })
    }

    override fun slettUtkast(ident: Ident, saksnummer: Saksnummer, periode: Periode?) {
        opplysninger.removeIf {
            it.ident == ident && it.saksnummer == saksnummer && (periode == null || it.timerArbeidet.dato in periode)
        }
    }
}