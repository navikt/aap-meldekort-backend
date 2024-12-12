package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import java.time.LocalDate
import java.util.*

private const val AAP_KODE = "ATTF" // attføringsstønad

class ArenaService(
    private val arena: Arena
) {
    fun meldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val person = arena.person(innloggetBruker)
        val meldekortListe = person ?.meldekortListe ?: emptyList()

        return meldekortListe
            .filter { meldekort ->
                meldekort.hoyesteMeldegruppe == AAP_KODE && meldekort.beregningstatus in arrayOf("OPPRE", "SENDT")
            }
            .map { meldekort ->
                val kanSendesFra = meldekort.tilDato.minusDays(1)
                Meldeperiode(
                    meldekortId = meldekort.meldekortId,
                    periode = Periode(meldekort.fraDato, meldekort.tilDato),
                    kanSendesFra = kanSendesFra,
                    kanSendes = !LocalDate.now().isBefore(kanSendesFra),
                    kanEndres = kanEndres(meldekort, meldekortListe),
                    status = Meldeperiode.Status.TIL_UTFYLLING,
                )
            }
    }


    /* Hva betyr egentlig dette her? Er denne relevant? */
    fun harMeldeplikt(innloggetBruker: InnloggetBruker): Boolean {
        return arena.meldegrupper(innloggetBruker).any { it.meldegruppeKode == AAP_KODE }
    }

    fun person(innloggetBruker: InnloggetBruker): Arena.Person? {
        return arena.person(innloggetBruker)
    }

    fun sendtemeldekort(innloggetBruker: InnloggetBruker) {
        val perioder = arena.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5)
            .meldekortListe
            ?.filter { meldekort -> meldekort.hoyesteMeldegruppe == AAP_KODE }
            ?.map { meldekort ->
                arena.meldekortdetaljer(innloggetBruker, meldekort.meldekortId)
            }
        // Domeneobjekt? Beholde noe mapping?
//
//                            val aktivitetsdager = mapAktivitetsdager(meldekort.fraDato, meldekortdetaljer)
//
//                            Rapporteringsperiode(
//                                meldekort.meldekortId,
//                                Periode(
//                                    meldekort.fraDato,
//                                    meldekort.tilDato
//                                ),
//                                aktivitetsdager,
//                                kanSendesFra,
//                                false,
//                                kanEndres(meldekort, person.meldekortListe),
//                                when (meldekort.beregningstatus) {
//                                    in arrayOf(
//                                        "FERDI",
//                                        "IKKE"
//                                    ) -> RapporteringsperiodeStatus.Ferdig
//
//                                    "OVERM" -> RapporteringsperiodeStatus.Endret
//                                    "FEIL" -> RapporteringsperiodeStatus.Feilet
//                                    else -> RapporteringsperiodeStatus.Innsendt
//                                },
//                                meldekort.mottattDato,
//                                meldekort.bruttoBelop.toDouble(),
//                                meldekortdetaljer.sporsmal?.arbeidssoker,
//                                meldekortdetaljer.begrunnelse
//                            )
//                        }
//

    }

    fun endrermeldekort(innloggetBruker: InnloggetBruker, meldekortId: Long): Long {
        return arena.korrigertMeldekort(innloggetBruker, meldekortId)
    }

    fun sendInn(innsendtMeldekort: InnsendtMeldekort, innloggetBruker: InnloggetBruker): InnsendingResponse {
        val meldekortdetaljer = arena.meldekortdetaljer(innloggetBruker, innsendtMeldekort.meldekortId)
        check(meldekortdetaljer.fodselsnr == innloggetBruker.ident)

        val meldekortdager = innsendtMeldekort.timerArbeidet.mapIndexed { offset, timerArbeidet ->
            Arena.MeldekortkontrollFravaer(
                dato = innsendtMeldekort.meldeperiode.fom.plusDays(offset.toLong()),
                arbeidTimer = timerArbeidet ?: 0.0,
            )
        }

        // Oppretter MeldekortkontrollRequest
        val meldekortkontrollRequest = Arena.MeldekortkontrollRequest(
            meldekortId = innsendtMeldekort.meldekortId,
            fnr = meldekortdetaljer.fodselsnr,
            personId = meldekortdetaljer.personId,
            kilde = AAP_KODE,
            kortType = meldekortdetaljer.kortType,
            meldedato = if (meldekortdetaljer.kortType == "KORRIGERT_ELEKTRONISK" && meldekortdetaljer.meldeDato != null) meldekortdetaljer.meldeDato else LocalDate.now(),
            periodeFra = innsendtMeldekort.meldeperiode.fom,
            periodeTil = innsendtMeldekort.meldeperiode.tom,
            meldegruppe = meldekortdetaljer.meldegruppe,
            arbeidet = innsendtMeldekort.harDuJobbet,
            begrunnelse = if (meldekortdetaljer.kortType == "KORRIGERT_ELEKTRONISK") TODO() else null,
            meldekortdager = meldekortdager
        )

        val meldekortkontrollResponse = arena.sendInn(innloggetBruker, meldekortkontrollRequest)

        return InnsendingResponse(
            id = meldekortkontrollResponse.meldekortId,
            status = if (meldekortkontrollResponse.kontrollStatus in arrayOf("OK", "OKOPP")) "OK" else "FEIL",
            feil = meldekortkontrollResponse.feilListe.map { feil -> InnsendingFeil(feil.kode, feil.params) }
        )
    }

    data class InnsendingResponse(
        val id: Long,
        val status: String,
        val feil: List<InnsendingFeil>
    )

    data class InnsendingFeil(
        val kode: String,
        val params: List<String>? = null
    )



    private fun kanEndres(meldekort: Arena.Meldekort, meldekortListe: List<Arena.Meldekort>): Boolean {
        return if (meldekort.kortType == "10" || meldekort.beregningstatus == "UBEHA") {
            false
        } else {
            meldekortListe.none { mk ->
                meldekort.meldekortId != mk.meldekortId &&
                        meldekort.meldeperiode == mk.meldeperiode &&
                        mk.kortType == "10"
            }
        }
    }

    data class Aktivitet(
        val uuid: UUID,
        val type: AktivitetsType,
        val timer: Double?
    ) {
        enum class AktivitetsType {
            Arbeid,
            Syk,
            Utdanning,
            Fravaer
        }
    }

    class Dag(
        val dato: LocalDate,
        val aktiviteter: List<Aktivitet> = emptyList(),
        val dagIndex: Int
    ) {
        fun finnesAktivitetMedType(aktivitetsType: Aktivitet.AktivitetsType): Boolean {
            return this.aktiviteter.find { aktivitet -> aktivitet.type == aktivitetsType } != null
        }

        fun hentArbeidstimer(): Double {
            return this.aktiviteter.find { aktivitet -> aktivitet.type == Aktivitet.AktivitetsType.Arbeid }?.timer ?: 0.0
        }
    }

    private fun mapAktivitetsdager(fom: LocalDate, meldekortdetaljer: Arena.Meldekortdetaljer): List<Dag> {
        val aktivitetsdager = List(14) { index ->
            Dag(fom.plusDays(index.toLong()), mutableListOf(), index)
        }
        meldekortdetaljer.sporsmal?.meldekortDager?.forEach { dag ->
            if (dag.arbeidetTimerSum != null && dag.arbeidetTimerSum > 0) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Arbeid,
                        dag.arbeidetTimerSum.toDouble()
                    )
                )
            }
            if (dag.syk == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Syk,
                        null
                    )
                )
            }
            if (dag.kurs == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Utdanning,
                        null
                    )
                )
            }
            if (dag.annetFravaer == true) {
                (aktivitetsdager[dag.dag - 1].aktiviteter as MutableList).add(
                    Aktivitet(
                        UUID.randomUUID(),
                        Aktivitet.AktivitetsType.Fravaer,
                        null
                    )
                )
            }
        }

        return aktivitetsdager
    }
}
