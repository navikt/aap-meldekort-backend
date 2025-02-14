package no.nav.aap.journalføring

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.Periode
import no.nav.aap.journalføring.DokarkivGateway.Journalposttype.INNGAAENDE
import no.nav.aap.journalføring.DokarkivGateway.Tema.AAP
import no.nav.aap.arena.Skjema
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class DokarkivService(
    private val dokarkivGateway: DokarkivGateway,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY")
    private var locale: Locale? = Locale.of("nb", "NO") // Vi skal regne ukenummer iht norske regler
    private val woy = WeekFields.of(locale).weekOfWeekBasedYear()
    private val BREVKODE = "NAV 00-10.02"
    private val BREVKODE_KORRIGERT = "NAV 00-10.03"

    // TODO: lag et dokument
    private val fysiskDokument =
        """
        %PDF-1.0
        1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
        2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
        3 0 obj<</Type/Page/Parent 2 0 R/Resources<<>>/MediaBox[0 0 9 9]>>endobj
        xref
        0 4
        0000000000 65535 f
        0000000009 00000 n
        0000000052 00000 n
        0000000101 00000 n
        trailer<</Root 1 0 R/Size 4>>
        startxref
        174
        %%EOF%
        """.trimIndent()

    fun journalpostForArena(
        skjema: Skjema,
        vårReferanse: String,
        datoMottatt: LocalDate?,
        kanSendesFra: LocalDate,
        korrigert: Boolean,
    ): DokarkivGateway.Journalpost {
        val tittel = lagTittel(skjema.meldeperiode, korrigert)

        return DokarkivGateway.Journalpost(
            journalposttype = INNGAAENDE,
            avsenderMottaker = DokarkivGateway.AvsenderMottaker(
                id = skjema.ident.asString,
                idType = DokarkivGateway.AvsenderIdType.FNR,
            ),
            bruker = DokarkivGateway.Bruker(
                id = skjema.ident.asString,
                idType = DokarkivGateway.BrukerIdType.FNR,
            ),
            tema = AAP,
            tittel = tittel,
            kanal = "NAV_NO",
            journalfoerendeEnhet = "9999",
            eksternReferanseId = vårReferanse,
            datoMottatt = (datoMottatt ?: LocalDate.now()).format(DateTimeFormatter.ISO_DATE),
            tilleggsopplysninger = listOf(
                DokarkivGateway.Tilleggsopplysning(
                    "meldekortId",
                    skjema.meldekortId.toString(),
                ),
                DokarkivGateway.Tilleggsopplysning(
                    "kortKanSendesFra",
                    kanSendesFra.format(dateFormatter)
                )
            ),
            sak = DokarkivGateway.Sak(
                sakstype = DokarkivGateway.Sakstype.GENERELL_SAK,
                // TODO - hent ut fagsakId via SakerGateway
                // sakstype = FAGSAK,
                // fagsaksystem = AO01,
                // fagsakId = sakId,
            ),
            dokumenter = listOf(
                DokarkivGateway.Dokument(
                    tittel = tittel,
                    brevkode = if (korrigert) BREVKODE_KORRIGERT else BREVKODE,
                    dokumentvarianter = listOf(
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.PDF,
                            variantformat = DokarkivGateway.Variantformat.ARKIV,
                            fysiskDokument = fysiskDokument.encodeToByteArray(),

                            ),
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.JSON,
                            variantformat = DokarkivGateway.Variantformat.ORIGINAL,
                            fysiskDokument = DefaultJsonMapper.toJson(skjema.payload).encodeToByteArray(),
                        )
                    ),
                )
            ),
        )
    }


    private fun lagTittel(meldeperiode: Periode, korrigert: Boolean): String {
        val uke1 = meldeperiode.fom.get(woy)
        val uke2 = meldeperiode.tom.get(woy)
        val fra = meldeperiode.fom.format(dateFormatter)
        val til = meldeperiode.tom.format(dateFormatter)
        val meldekort = if (korrigert) "Korrigert meldekort" else "Meldekort"
        return "$meldekort for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av NAV"
    }

    fun journalfør(journalpost: DokarkivGateway.Journalpost) {
        val response = dokarkivGateway.oppdater(journalpost, forsøkFerdigstill = true)
        check(response.journalpostferdigstilt)
    }

}