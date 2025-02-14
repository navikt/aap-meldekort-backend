package no.nav.aap.meldekort.joark

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.journalføring.JoarkClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JoarkClientImplTest {
    @Test
    fun `base64 encode av dokument`() {
        val journalpost = JoarkClient.Journalpost(
            journalposttype = JoarkClient.Journalposttype.INNGAAENDE,
            dokumenter = listOf(
                JoarkClient.Dokument(
                dokumentvarianter = listOf(
                    JoarkClient.DokumentVariant(
                        filtype = JoarkClient.Filetype.PDF,
                        variantformat = JoarkClient.Variantformat.ARKIV,
                        fysiskDokument = "Hello world".encodeToByteArray()
                    )
                )
            ))
        )
       assertTrue(DefaultJsonMapper.toJson(journalpost).contains("SGVsbG8gd29ybGQ="))
    }

    @Test
    fun `base64 decode av dokument`() {
        val journalpost = """
            {
                "journalposttype": "INNGAAENDE",
                "dokumenter": [
                    {
                        "dokumentvarianter": [
                            {
                                "filtype": "PDF",
                                "variantformat": "ARKIV",
                                "fysiskDokument": "SGVsbG8gd29ybGQ="
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val fysiskDokument = DefaultJsonMapper.fromJson<JoarkClient.Journalpost>(journalpost)
            .dokumenter!![0].dokumentvarianter[0].fysiskDokument
            .decodeToString()

        assertEquals("Hello world", fysiskDokument)
    }
}