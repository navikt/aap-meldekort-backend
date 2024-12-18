package no.nav.aap.meldekort.journalføring

import no.nav.aap.meldekort.Periode
import no.nav.aap.meldekort.journalføring.JoarkClient.Journalposttype.INNGAAENDE
import no.nav.aap.meldekort.journalføring.JoarkClient.Tema.AAP
import no.nav.aap.meldekort.arena.Skjema
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class JoarkService(
    private val joarkClient: JoarkClient,
    private val joarkRepository: JoarkRepository,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY")
    private val dateFormat = dateFormatter
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm")
    private var locale: Locale? = Locale.of("nb", "NO") // Vi skal regne ukenummer iht norske regler
    private val woy = WeekFields.of(locale).weekOfWeekBasedYear()

    fun journalpostForArena(
        skjema: Skjema,
        vårReferanse: String,
        datoMottatt: LocalDate?,
        kanSendesFra: LocalDate,
        base64json: String,
        korrigert: Boolean,
    ): JoarkClient.Journalpost {
        val tittel = lagTittel(skjema.meldeperiode, korrigert)

        return JoarkClient.Journalpost(
            journalposttype = INNGAAENDE,
            avsenderMottaker = JoarkClient.AvsenderMottaker(
                id = skjema.ident.asString,
                idType = JoarkClient.AvsenderIdType.FNR,
            ),
            bruker = JoarkClient.Bruker(
                id = skjema.ident.asString,
                idType = JoarkClient.BrukerIdType.FNR,
            ),
            tema = AAP,
            behandlingstema = null, // TODO: burde det være noe her?
            tittel = tittel,
            kanal = "NAV_NO",
            journalfoerendeEnhet = "9999",
            eksternReferanseId = vårReferanse,
            datoMottatt = (datoMottatt ?: LocalDate.now()).format(DateTimeFormatter.ISO_DATE),
            tilleggsopplysninger = listOf(
                // Nøkkel - maksimum 20 tegn
                JoarkClient.Tilleggsopplysning(
                    "meldekortId",
                    skjema.meldekortId.toString(),
                ),
                JoarkClient.Tilleggsopplysning(
                    "kortKanSendesFra",
                    kanSendesFra.format(dateFormat)
                )
            ),
            sak = JoarkClient.Sak(
                sakstype = JoarkClient.Sakstype.GENERELL_SAK,
                // TODO:
                // sakstype = JoarkClient.Sakstype.FAGSAK,
                // fagsaksystem = JoarkClient.FagsaksSystem.AO01,
                // fagsakId = sakId,
            ),
            dokumenter = listOf(
                JoarkClient.Dokument(
                    tittel = tittel,
                    brevkode = if (korrigert) "NAV 00-10.03" else "NAV 00-10.02",
                    dokumentvarianter = listOf(
                        // TODO: HTML? PDF?
                        JoarkClient.DokumentVariant(
                            filtype = JoarkClient.Filetype.JSON,
                            variantformat = JoarkClient.Variantformat.ORIGINAL,
                            fysiskDokument = base64json,
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

    fun journalfør(journalpost: JoarkClient.Journalpost) {
        val response = joarkClient.oppdater(journalpost, forsøkFerdigstill = true)
        check(response.journalpostferdigstilt)
    }

}