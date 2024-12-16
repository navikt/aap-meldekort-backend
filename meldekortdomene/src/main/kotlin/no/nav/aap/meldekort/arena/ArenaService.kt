package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.ArenaMeldekort.Companion.tilMeldeperioder
import no.nav.aap.meldekort.arenaflyt.InnsendtMeldekort
import no.nav.aap.meldekort.arenaflyt.MeldekortRepository
import no.nav.aap.meldekort.arenaflyt.Meldeperiode


class ArenaService(
    private val arenaClient: ArenaClient,
    private val meldekortRepository: MeldekortRepository,
) {
    fun meldeperioder(innloggetBruker: InnloggetBruker): List<Meldeperiode> {
        val innsendteMeldekort = meldekortRepository.loadMeldekort().map { it.meldekortId }
        val meldekort = (arenaClient.person(innloggetBruker)?.arenaMeldekortListe ?: emptyList())
            .map { if (it.meldekortId in innsendteMeldekort) it.copy(historisk = true) else it }

        val historiskeMeldekort = arenaClient.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5).arenaMeldekortListe

        return (meldekort + historiskeMeldekort).tilMeldeperioder()
    }

    /* Hva betyr egentlig dette her? Er denne relevant? */
    fun harMeldeplikt(innloggetBruker: InnloggetBruker): Boolean {
        return arenaClient.meldegrupper(innloggetBruker).any { it.erMeldegruppeAAP() }
    }

    fun person(innloggetBruker: InnloggetBruker): ArenaPerson? {
        return arenaClient.person(innloggetBruker)
    }

    fun sendtemeldekort(innloggetBruker: InnloggetBruker) {
        val perioder = arenaClient.historiskeMeldekort(innloggetBruker, antallMeldeperioder = 5)
            .arenaMeldekortListe
            ?.filter { meldekort -> meldekort.hoyesteMeldegruppe == AAP_KODE }
            ?.map { meldekort ->
                arenaClient.meldekortdetaljer(innloggetBruker, meldekort.meldekortId)
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
        return arenaClient.korrigertMeldekort(innloggetBruker, meldekortId)
    }

    fun sendInn(innsendtMeldekort: InnsendtMeldekort, innloggetBruker: InnloggetBruker) {
        val meldekortdetaljer = arenaClient.meldekortdetaljer(innloggetBruker, innsendtMeldekort.meldekortId)
        check(meldekortdetaljer.fodselsnr == innloggetBruker.ident)

        val arenaMeldekortkontrollRequest = ArenaMeldekortkontrollRequest.konstruer(innsendtMeldekort, meldekortdetaljer)

        val meldekortkontrollResponse = arenaClient.sendInn(innloggetBruker, arenaMeldekortkontrollRequest)
        meldekortkontrollResponse.validerVellykket()
    }
}
