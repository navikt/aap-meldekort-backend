package no.nav.aap.journalføring

import no.nav.aap.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.journalføring.DokarkivGateway.Journalposttype.INNGAAENDE
import no.nav.aap.journalføring.DokarkivGateway.Tema.AAP
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.SakService
import no.nav.aap.utfylling.Utfylling
import java.time.ZoneId

class JournalføringService(
    private val dokarkivGateway: DokarkivGateway,
    private val flytJobbRepository: FlytJobbRepository,
    private val pdfgenGateway: PdfgenGateway,
) {
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
        sakService: SakService,
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

        val fagsystemspesifikkeOpplysninger = sakService.opplysningerForJournalpost(utfylling)
        val journalpost = journalpost(
            ident = ident,
            utfylling = utfylling,
            fagsak = sakService.sak.referanse,
            meldekort = meldekort,
            pdf = pdfgenGateway.genererPdf(
                ident = ident,
                mottatt = utfylling.sistEndret,
                meldekort = meldekort,
                utfylling = utfylling
            ),
            fagsystemspesifikkeOpplysninger = fagsystemspesifikkeOpplysninger
        )

        val response = dokarkivGateway.oppdater(
            journalpost,
            forsøkFerdigstill = fagsystemspesifikkeOpplysninger.ferdigstill
        )
        if (fagsystemspesifikkeOpplysninger.ferdigstill) {
            check(response.journalpostferdigstilt)
        }
    }

    private fun journalpost(
        ident: Ident,
        fagsystemspesifikkeOpplysninger: SakService.OpplysningerForJournalpost,
        utfylling: Utfylling,
        fagsak: FagsakReferanse?,
        meldekort: Meldekort,
        pdf: ByteArray,
    ): DokarkivGateway.Journalpost {
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
            tittel = fagsystemspesifikkeOpplysninger.tittel,
            kanal = "NAV_NO",
            journalfoerendeEnhet = if (fagsystemspesifikkeOpplysninger.ferdigstill)
                "9999" /* 9999 = automatisk behandling */
            else
                null,
            eksternReferanseId = utfylling.referanse.asUuid.toString(),
            datoMottatt = utfylling.sistEndret.toString(),
            tilleggsopplysninger = fagsystemspesifikkeOpplysninger
                .tilleggsopplysning
                .entries
                .map { DokarkivGateway.Tilleggsopplysning(it.key, it.value) },
            sak = fagsystemspesifikkeOpplysninger.journalførPåSak,
            dokumenter = listOf(
                DokarkivGateway.Dokument(
                    tittel = fagsystemspesifikkeOpplysninger.tittel,
                    brevkode = fagsystemspesifikkeOpplysninger.brevkode,
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
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                dokarkivGateway = GatewayProvider.provide(),
                pdfgenGateway = GatewayProvider.provide(),
                flytJobbRepository = FlytJobbRepository(connection),
            )
        }
    }
}