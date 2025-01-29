package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.OPPRE
import no.nav.aap.meldekort.arena.ArenaMeldekort.ArenaStatus.SENDT
import no.nav.aap.meldekort.arena.MeldekortStatus.INNSENDT


class MeldekortService(
    private val arenaClient: ArenaClient,
    private val meldekortRepository: MeldekortRepository,
) {
    fun alleMeldekort(innloggetBruker: InnloggetBruker): List<Meldekort>? {
        val kommendeMeldekort = kommendeMeldekort(innloggetBruker) ?: return null
        return (kommendeMeldekort + historiskeMeldekort(innloggetBruker))
            .sortedBy { it.periode.fom }
    }

    fun kommendeMeldekort(innloggetBruker: InnloggetBruker): List<KommendeMeldekort>? {
        val arenaMeldekort = arenaClient.person(innloggetBruker)?.arenaMeldekortListe ?: return null
        val dbMeldekort = meldekortRepository.hent(innloggetBruker.ident, arenaMeldekort.map { it.meldekortId })

        return arenaMeldekort
            .asSequence()
            .filter { it.erForAap }
            /* TODO: Er neste linje (filter != KORRIGERING) riktig? */
            .filter { it.type != MeldekortType.KORRIGERING /* Kort for korrigering opprettes samtidig som det sendes inn */ }
            .filter { it.beregningstatus in arrayOf(OPPRE, SENDT) }
            .filterNot { meldekort -> erSendtInn(dbMeldekort, meldekort) }
            .map { meldekort ->
                KommendeMeldekort(
                    meldekortId = meldekort.meldekortId,
                    periode = Periode(meldekort.fraDato, meldekort.tilDato),
                    type = meldekort.type,
                    kanKorrigeres = !meldekort.erUbehandletEllerKorrigeringForPerioden(arenaMeldekort)
                )
            }
            .toList()
            .also { meldekortRepository.upsert(innloggetBruker.ident, it) }
    }

    private fun erSendtInn(dbMeldekort: List<Meldekort>, meldekort: ArenaMeldekort): Boolean {
        return dbMeldekort.singleOrNull { it.meldekortId == meldekort.meldekortId } is HistoriskMeldekort
    }

    private fun historiskeMeldekortFraArena(innloggetBruker: InnloggetBruker): List<HistoriskMeldekort> {
        val råMeldekortFraArena =
            arenaClient.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5).arenaMeldekortListe

        val detaljerCache = mutableMapOf<Long, ArenaMeldekortdetaljer>()
        fun detaljer(meldekortId: Long) = detaljerCache.computeIfAbsent(meldekortId) {
            arenaClient.meldekortdetaljer(innloggetBruker, meldekortId)
        }
        return råMeldekortFraArena
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
                        beregningStatus = status,
                        type = it.type,
                        begrunnelseEndring = detaljer(it.meldekortId).begrunnelse?.ifBlank { null },
                        mottattIArena = it.mottattDato,
                        originalMeldekortId = if (it.meldekortId != orginalMeldekortId) orginalMeldekortId else null,
                        bruttoBeløp = it.bruttoBelop
                    )
                }
            }
    }

    fun historiskeMeldekort(innloggetBruker: InnloggetBruker): List<HistoriskMeldekort> {
        val meldekortFraArena = historiskeMeldekortFraArena(innloggetBruker)
        val meldekortFraDatabasen = meldekortRepository.hentAlleHistoriskeMeldekort(innloggetBruker.ident)
        val lagredeMeldekort = meldekortFraDatabasen.asSequence().map { it.meldekortId }.toSet()

        meldekortRepository.upsert(innloggetBruker.ident, meldekortFraArena.filter { it.meldekortId !in lagredeMeldekort})

        val meldekortene = meldekortFraArena.toMutableList()
        meldekortFraDatabasen
            .forEach { meldekortFraDb ->
                val meldekortIndex = meldekortene.indexOfFirst { it.meldekortId == meldekortFraDb.meldekortId }
                val meldekortFraArena = meldekortene.getOrNull(meldekortIndex)
                if (meldekortFraArena == null) {
                    meldekortene.add(meldekortFraDb)
                } else if (meldekortFraArena.erLengreIProsessen(meldekortFraDb)) {
                    meldekortRepository.upsert(innloggetBruker.ident, meldekortFraArena)
                } else {
                    meldekortene[meldekortIndex] = meldekortFraDb
                }
            }
        return meldekortene
    }

    fun nyttMeldekortForKorrigering(
        innloggetBruker: InnloggetBruker,
        originalMeldekortId: Long
    ): HistoriskMeldekort {
        val originaltMeldekort = historiskeMeldekort(innloggetBruker).single { it.meldekortId == originalMeldekortId }
        check(originaltMeldekort.kanKorrigeres) { "Korrigering er ikke tillatt på meldekort med id $originalMeldekortId" }

        val meldekortId = arenaClient.korrigertMeldekort(innloggetBruker, originalMeldekortId)

        return HistoriskMeldekort(
            meldekortId = meldekortId,
            type = MeldekortType.KORRIGERING,
            periode = originaltMeldekort.periode,
            kanKorrigeres = false,
            begrunnelseEndring = null,
            mottattIArena = null,
            originalMeldekortId = originalMeldekortId,
            beregningStatus = INNSENDT,
            bruttoBeløp = null,
        ).also {
            meldekortRepository.upsert(innloggetBruker.ident, it)
        }
    }

    fun sendInn(skjema: Skjema, innloggetBruker: InnloggetBruker) {
        // TODO: Håndtere at systemet krasjer mellom de forskjellige stegene.
        val meldekortdetaljer = arenaClient.meldekortdetaljer(innloggetBruker, skjema.meldekortId)
        check(meldekortdetaljer.fodselsnr == innloggetBruker.ident.asString)

        val arenaMeldekortkontrollRequest = ArenaMeldekortkontrollRequest.konstruer(skjema, meldekortdetaljer)
        // TODO: Hvis det er korrigering, må vi opprette selve meldekortet her, før vi prøver å sende det inn.
//        arenaClient.korrigertMeldekort()

        val meldekortkontrollResponse = arenaClient.sendInn(innloggetBruker, arenaMeldekortkontrollRequest)
        meldekortkontrollResponse.validerVellykket()

        meldekortRepository.upsert(
            innloggetBruker.ident,
            HistoriskMeldekort(
                meldekortId = skjema.meldekortId,
                type = meldekortdetaljer.kortType.meldekortType,
                periode = skjema.meldeperiode,
                kanKorrigeres = false,  // TODO: når vi har flyt for korrigering må denne settes
                begrunnelseEndring = arenaMeldekortkontrollRequest.begrunnelse,
                mottattIArena = null,
                originalMeldekortId = null, // TODO: når vi har flyt for korrigering må det propageres her
                beregningStatus = INNSENDT,
                bruttoBeløp = null
            )
        )
    }
}
