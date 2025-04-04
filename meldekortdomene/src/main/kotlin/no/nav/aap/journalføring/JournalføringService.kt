package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.journalføring.DokarkivGateway.Journalposttype.INNGAAENDE
import no.nav.aap.journalføring.DokarkivGateway.Tema.AAP
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlytNavn
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class JournalføringService(
    private val dokarkivGateway: DokarkivGateway,
    private val flytJobbRepository: FlytJobbRepository,
    private val pdfgenGateway: PdfgenGateway,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        gatewayProvider.provide(),
        repositoryProvider.provide(),
        gatewayProvider.provide(),
    )

    fun bestillJournalføring(ident: Ident, utfylling: Utfylling) {
        flytJobbRepository.leggTil(
            JournalføringJobbUtfører.jobbInput(
                ident = ident,
                utfylling = utfylling.referanse,
                fagsak = utfylling.fagsak,
            )
        )
    }

    fun journalfør(
        ident: Ident,
        utfylling: Utfylling,
        sak: Sak,
    ) {
        val meldekort = MeldekortV0(
            harDuArbeidet = utfylling.svar.harDuJobbet!!,
            timerArbeidPerPeriode = utfylling.svar.timerArbeidet.map {
                ArbeidIPeriodeV0(
                    fraOgMedDato = it.dato,
                    tilOgMedDato = it.dato,
                    timerArbeid = it.timer ?: 0.0,
                )
            }
        )

        val journalpost = journalpost(
            ident = ident,
            utfylling = utfylling,
            meldekort = meldekort,
            pdf = pdfgenGateway.genererPdf(
                ident = ident,
                mottatt = utfylling.sistEndret,
                meldekort = meldekort,
                utfylling = utfylling
            ),
            sak = sak,
        )

        val forsøkFerdigstill = when (sak.referanse.system) {
            FagsystemNavn.ARENA -> true
            FagsystemNavn.KELVIN -> false
        }

        val response = dokarkivGateway.oppdater(
            journalpost,
            forsøkFerdigstill = forsøkFerdigstill
        )
        if (forsøkFerdigstill) {
            check(response.journalpostferdigstilt)
        }
    }

    private fun journalpost(
        ident: Ident,
        utfylling: Utfylling,
        meldekort: Meldekort,
        pdf: ByteArray,
        sak: Sak,
    ): DokarkivGateway.Journalpost {
        val uke1 = utfylling.periode.fom.get(uke)
        val uke2 = utfylling.periode.tom.get(uke)
        val fra = utfylling.periode.fom.format(dateFormatter)
        val til = utfylling.periode.tom.format(dateFormatter)
        val tittelsuffix = "for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av NAV"
        val tittel = when (utfylling.flyt) {
            UtfyllingFlytNavn.ARENA_VANLIG_FLYT,
            UtfyllingFlytNavn.AAP_FLYT ->
                "Meldekort $tittelsuffix"

            UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT,
            UtfyllingFlytNavn.AAP_KORRIGERING_FLYT ->
                "Korrigert meldekort $tittelsuffix"
        }

        return DokarkivGateway.Journalpost(
            journalposttype = INNGAAENDE,
            avsenderMottaker = DokarkivGateway.AvsenderMottaker(
                id = ident.asString,
                idType = DokarkivGateway.AvsenderIdType.FNR,
            ),
            bruker = DokarkivGateway.Bruker(
                id = ident.asString,
                idType = DokarkivGateway.BrukerIdType.FNR,
            ),
            tema = AAP,
            tittel = tittel,
            kanal = "NAV_NO",
            journalfoerendeEnhet = when (sak.referanse.system) {
                FagsystemNavn.ARENA ->
                    /* 9999 = automatisk behandling */
                    "9999"

                FagsystemNavn.KELVIN -> null
            },
            eksternReferanseId = utfylling.referanse.asUuid.toString(),
            datoMottatt = utfylling.sistEndret.toString(),
            tilleggsopplysninger = when (sak.referanse.system) {
                FagsystemNavn.ARENA -> listOf(
                    // TODO:
                    // "meldekortId" to skjema.meldekortId.toString(),
                    // "kortKanSendesFra" to kanSendesFra.format(dateFormatter),
                )

                FagsystemNavn.KELVIN -> emptyList()
            },
            sak = when (sak.referanse.system) {
                FagsystemNavn.KELVIN -> null
                FagsystemNavn.ARENA -> DokarkivGateway.Sak(
                    sakstype = DokarkivGateway.Sakstype.FAGSAK,
                    fagsaksystem = DokarkivGateway.FagsaksSystem.AO01,
                    fagsakId = sak.referanse.nummer.asString,
                )
            },
            dokumenter = listOf(
                DokarkivGateway.Dokument(
                    tittel = tittel,
                    brevkode = when (utfylling.flyt) {
                        UtfyllingFlytNavn.ARENA_VANLIG_FLYT,
                        UtfyllingFlytNavn.AAP_FLYT ->
                            "NAV 00-10.02"

                        UtfyllingFlytNavn.ARENA_KORRIGERING_FLYT,
                        UtfyllingFlytNavn.AAP_KORRIGERING_FLYT ->
                            "NAV 00-10.03"
                    },
                    dokumentvarianter = listOf(
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.PDF,
                            variantformat = DokarkivGateway.Variantformat.ARKIV,
                            fysiskDokument = pdf,
                        ),
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.JSON,
                            variantformat = DokarkivGateway.Variantformat.ORIGINAL,
                            fysiskDokument = DefaultJsonMapper.toJson(meldekort).encodeToByteArray(),
                        )
                    ),
                )
            ),
        )
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY")
        private val uke = WeekFields.of(Locale.of("nb", "NO")).weekOfWeekBasedYear()
    }
}