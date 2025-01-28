package no.nav.aap.meldekort.journalføring

import no.nav.aap.komponenter.json.DefaultJsonMapper
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
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY")
    private var locale: Locale? = Locale.of("nb", "NO") // Vi skal regne ukenummer iht norske regler
    private val woy = WeekFields.of(locale).weekOfWeekBasedYear()
    private val BREVKODE = "NAV 00-10.02"
    private val BREVKODE_KORRIGERT = "NAV 00-10.03"

    fun journalpostForArena(
        skjema: Skjema,
        vårReferanse: String,
        datoMottatt: LocalDate?,
        kanSendesFra: LocalDate,
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
            tittel = tittel,
            kanal = "NAV_NO",
            journalfoerendeEnhet = "9999",
            eksternReferanseId = vårReferanse,
            datoMottatt = (datoMottatt ?: LocalDate.now()).format(DateTimeFormatter.ISO_DATE),
            tilleggsopplysninger = listOf(
                JoarkClient.Tilleggsopplysning(
                    "meldekortId",
                    skjema.meldekortId.toString(),
                ),
                JoarkClient.Tilleggsopplysning(
                    "kortKanSendesFra",
                    kanSendesFra.format(dateFormatter)
                )
            ),
            sak = JoarkClient.Sak(
                sakstype = JoarkClient.Sakstype.GENERELL_SAK,
                // TODO - ønsker å journalføre som fagsak, men har ikke sakId:
                // sakstype = JoarkClient.Sakstype.FAGSAK,
                // fagsaksystem = JoarkClient.FagsaksSystem.AO01,
                // fagsakId = sakId,
            ),
            dokumenter = listOf(
                JoarkClient.Dokument(
                    tittel = tittel,
                    brevkode = if (korrigert) BREVKODE_KORRIGERT else BREVKODE,
                    dokumentvarianter = listOf(
                        JoarkClient.DokumentVariant(
                            filtype = JoarkClient.Filetype.JSON,
                            variantformat = JoarkClient.Variantformat.ORIGINAL,
                            fysiskDokument = DefaultJsonMapper.toJson(skjema.payload),
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