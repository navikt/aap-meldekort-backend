package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.OPPRE
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.SENDT
import no.nav.aap.meldekort.arena.MeldekortStatus.INNSENDT
import java.time.LocalDate


class MeldekortService(
    private val arenaClient: ArenaClient,
    private val meldekortRepository: MeldekortRepository,
    /* TODO: finn ut hva dette er. */ private val innsendingstidspunktProvider: (String) -> Long = { -1 },
) {
    fun alleMeldekort(innloggetBruker: InnloggetBruker): List<Meldekort> {
        return kommendeMeldekort(innloggetBruker) + historiskeMeldekort(innloggetBruker)
    }

    fun kommendeMeldekort(innloggetBruker: InnloggetBruker): List<KommendeMeldekort> {
        val arenaMeldekort = arenaClient.person(innloggetBruker)?.arenaMeldekortListe.orEmpty()
        val dbMeldekort = meldekortRepository.hent(innloggetBruker.ident, arenaMeldekort.map { it.meldekortId })

        return arenaMeldekort
            .asSequence()
            .filter { it.erForAap }
            /* TODO: Er neste linje (filter != KORRIGERING) riktig? */
            .filter { it.type != MeldekortType.KORRIGERING /* Kort for korrigering opprettes samtidig som det sendes inn */}
            .filter { it.beregningstatus in arrayOf(OPPRE, SENDT) }
            .filter { meldekort -> dbMeldekort.none { it.meldekortId == meldekort.meldekortId && it is HistoriskMeldekort } }
            .map { meldekort ->
                KommendeMeldekort(
                    meldekortId = meldekort.meldekortId,
                    periode = Periode(meldekort.fraDato, meldekort.tilDato),
                    type = meldekort.type,
                    kanSendesFra = meldekort.tilDato.plusDays(innsendingstidspunktProvider(meldekort.periodekode)),
                    kanKorrigeres = !meldekort.erUbehandletEllerKorrigeringForPerioden(arenaMeldekort)
                )
            }
            .toList()
            .also { meldekortRepository.upsertFraArena(innloggetBruker.ident, it) }
    }

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker): List<HistoriskMeldekort> {
        val råMeldekortFraArena = arenaClient.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5).arenaMeldekortListe

        val detaljerCache = mutableMapOf<Long, ArenaMeldekortdetaljer>()
        fun detaljer(meldekortId: Long) = detaljerCache.computeIfAbsent(meldekortId) {
            arenaClient.meldekortdetaljer(innloggetBruker, meldekortId)
        }

        val meldekortene = råMeldekortFraArena
            .filter { it.erForAap }
            .groupBy { it.fraDato }
            .flatMap { (_, meldekortene) ->
                val orginalMeldekortId = meldekortene.asSequence()
                    .filter { detaljer(it.meldekortId).begrunnelse.isNullOrBlank() }
                    .minByOrNull { it.mottattDato ?: throw RuntimeException("Innsendt periode har ikke mottatt dato") }
                    ?.meldekortId

                meldekortene.map {
                    val status = it.beregningstatus.historiskStatus
                    HistoriskMeldekort(
                        meldekortId = it.meldekortId,
                        periode = Periode(it.fraDato, it.tilDato),
                        kanKorrigeres = !it.erUbehandletEllerKorrigeringForPerioden(råMeldekortFraArena),
                        status = status,
                        type = it.type,
                        begrunnelseEndring = detaljer(it.meldekortId).begrunnelse?.ifBlank { null },
                        mottatt = it.mottattDato,
                        orginalMeldekortId = if (it.meldekortId != orginalMeldekortId) orginalMeldekortId else null,
                    )
                }
            }
            .toMutableList()

        meldekortRepository.hentAlleHistoriskeMeldekort(innloggetBruker.ident)
            .forEach { meldekortFraDb ->
                val periodeIndex = meldekortene.indexOfFirst { it.meldekortId == meldekortFraDb.meldekortId }
                if (periodeIndex < 0) {
                    meldekortene.add(meldekortFraDb)
                } else if (meldekortFraDb.status.ordinal >= meldekortene[periodeIndex].status.ordinal) {
                    meldekortene[periodeIndex] = meldekortFraDb
                }
            }

        val idag = LocalDate.now()
        val nåværendePeriode = meldekortene.filter { it.periode.inneholder(idag) }
        val sisteFemPerioderSortert =
            meldekortene
                .filterNot { it in nåværendePeriode }
                .groupBy { it.periode.fom }
                .toSortedMap(compareByDescending { it })
                .entries
                .take(5)
                .associate { it.toPair() }
                .values
                .flatten()
                .sortedWith(
                    compareByDescending<HistoriskMeldekort> { it.periode.fom }
                        .thenByDescending { it.mottatt },
                )
        return nåværendePeriode + sisteFemPerioderSortert
    }

    fun sendInn(skjema: Skjema, innloggetBruker: InnloggetBruker) {
        // TODO: Håndtere at systemet krasjer mellom de forskjellige stegene.
        // TODO: Hvis det er korrigering, må vi opprette selve meldekortet her, før vi prøver å sende det inn.
        val meldekortdetaljer = arenaClient.meldekortdetaljer(innloggetBruker, skjema.meldekortId)
        check(meldekortdetaljer.fodselsnr == innloggetBruker.ident.asString)

        val arenaMeldekortkontrollRequest = ArenaMeldekortkontrollRequest.konstruer(skjema, meldekortdetaljer)

        val meldekortkontrollResponse = arenaClient.sendInn(innloggetBruker, arenaMeldekortkontrollRequest)
        meldekortkontrollResponse.validerVellykket()

        meldekortRepository.oppdater(innloggetBruker.ident, HistoriskMeldekort(
            meldekortId = skjema.meldekortId,
            type = meldekortdetaljer.kortType.meldekortType,
            periode = skjema.meldeperiode,
            kanKorrigeres = false,  // TODO: når vi har flyt for korrigering må denne settes
            begrunnelseEndring = arenaMeldekortkontrollRequest.begrunnelse,
            mottatt = null,
            orginalMeldekortId = null, // TODO: når vi har flyt for korrigering må det propageres her
            status = INNSENDT,
        ))
    }
}
