package no.nav.aap.arena

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaMeldekort.ArenaStatus.OPPRE
import no.nav.aap.arena.ArenaMeldekort.ArenaStatus.SENDT
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MeldekortService(
    private val arenaGateway: ArenaGateway,
    private val meldekortRepository: MeldekortRepository,
) {
    private val log = LoggerFactory.getLogger(this::class.java)!!

//    fun hentLokaltMeldekort(ident: Ident, meldekortId: MeldekortId): Meldekort? {
//        return meldekortRepository.hent(ident, meldekortId)
//    }

//    fun alleMeldekort(innloggetBruker: InnloggetBruker): List<Meldekort>? {
//        val kommendeMeldekort = kommendeMeldekort(innloggetBruker) ?: return null
//        return (kommendeMeldekort + historiskeMeldekort(innloggetBruker))
//            .sortedBy { it.periode.fom }
//    }

    fun kommendeMeldekort(innloggetBruker: InnloggetBruker): List<KommendeMeldekort>? {
        val arenaMeldekort = arenaGateway.person(innloggetBruker)?.arenaMeldekortListe ?: return null
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
            arenaGateway.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5).arenaMeldekortListe

        val detaljerCache = mutableMapOf<MeldekortId, ArenaMeldekortdetaljer>()
        fun detaljer(meldekortId: MeldekortId) = detaljerCache.computeIfAbsent(meldekortId) {
            arenaGateway.meldekortdetaljer(innloggetBruker, meldekortId)
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

        meldekortRepository.upsert(
            innloggetBruker.ident,
            meldekortFraArena.filter { it.meldekortId !in lagredeMeldekort })

        val meldekortene = meldekortFraArena.toMutableList()
        meldekortFraDatabasen
            .forEach { meldekortFraDb ->
                val meldekortIndex = meldekortene.indexOfFirst { it.meldekortId == meldekortFraDb.meldekortId }
                val meldekortFraArena = meldekortene.getOrNull(meldekortIndex)
                if (meldekortFraArena == null) {
                    meldekortene.add(meldekortFraDb)
                } else if (meldekortFraArena.erLengreIProsessen(meldekortFraDb)) {
                    log.info("upserter ${meldekortFraArena.meldekortId} arena-status: ${meldekortFraArena.beregningStatus}, db-status: ${meldekortFraDb.beregningStatus}")
                    meldekortRepository.upsert(innloggetBruker.ident, meldekortFraArena)
                } else {
                    meldekortene[meldekortIndex] = meldekortFraDb
                }
            }
        return meldekortene
    }

    fun gjeldeneHistoriskeMeldekort(
        innloggetBruker: InnloggetBruker,
        meldeperiode: Periode
    ): HistoriskMeldekort? {
        return historiskeMeldekort(innloggetBruker).filter { it.periode == meldeperiode }
            .maxByOrNull {
                /*  TODO: finn riktig sortering */
                it.mottattIArena ?: LocalDate.MAX
            }
    }


//    fun nyttMeldekortForKorrigering(
//        innloggetBruker: InnloggetBruker,
//        originalMeldekortId: MeldekortId
//    ): HistoriskMeldekort {
//        val originaltMeldekort = historiskeMeldekort(innloggetBruker).single { it.meldekortId == originalMeldekortId }
//        check(originaltMeldekort.kanKorrigeres) { "Korrigering er ikke tillatt på meldekort med id $originalMeldekortId" }
//
//        val meldekortId = arenaGateway.korrigertMeldekort(innloggetBruker, originalMeldekortId)
//        return HistoriskMeldekort(
//            meldekortId = meldekortId,
//            type = MeldekortType.KORRIGERING,
//            periode = originaltMeldekort.periode,
//            kanKorrigeres = false,
//            begrunnelseEndring = null,
//            mottattIArena = null,
//            originalMeldekortId = originalMeldekortId,
//            beregningStatus = INNSENDT,
//            bruttoBeløp = null,
//        ).also {
//            meldekortRepository.upsert(innloggetBruker.ident, listOf(it, originaltMeldekort.copy(kanKorrigeres = false)))
//        }
//    }

//    fun sendInn(skjema: Skjema, innloggetBruker: InnloggetBruker) {
//        // TODO: Håndtere at systemet krasjer mellom de forskjellige stegene.
//        val meldekortdetaljer = arenaGateway.meldekortdetaljer(innloggetBruker, skjema.meldekortId)
//        check(meldekortdetaljer.fodselsnr == innloggetBruker.ident.asString)
//
//        val arenaMeldekortkontrollRequest = ArenaMeldekortkontrollRequest.konstruer(skjema, meldekortdetaljer)
//        // TODO: Hvis det er korrigering, må vi opprette selve meldekortet her, før vi prøver å sende det inn.
//
//        val meldekortkontrollResponse = arenaGateway.sendInn(innloggetBruker, arenaMeldekortkontrollRequest)
//        meldekortkontrollResponse.validerVellykket()
//
//        meldekortRepository.upsert(
//            innloggetBruker.ident,
//            HistoriskMeldekort(
//                meldekortId = skjema.meldekortId,
//                type = meldekortdetaljer.kortType.meldekortType,
//                periode = skjema.meldeperiode,
//                kanKorrigeres = false,  // TODO: når vi har flyt for korrigering må denne settes
//                begrunnelseEndring = arenaMeldekortkontrollRequest.begrunnelse,
//                mottattIArena = null,
//                originalMeldekortId = null, // TODO: når vi har flyt for korrigering må det propageres her
//                beregningStatus = INNSENDT,
//                bruttoBeløp = null
//            )
//        )
//    }
}
