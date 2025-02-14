package no.nav.aap.meldekort.journalføring

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.journalføring.DokarkivGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DokarkivGatewayImplTest {
    @Test
    fun `base64 encode av dokument`() {
        val journalpost = DokarkivGateway.Journalpost(
            journalposttype = DokarkivGateway.Journalposttype.INNGAAENDE,
            dokumenter = listOf(
                DokarkivGateway.Dokument(
                dokumentvarianter = listOf(
                    DokarkivGateway.DokumentVariant(
                        filtype = DokarkivGateway.Filetype.PDF,
                        variantformat = DokarkivGateway.Variantformat.ARKIV,
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

        val fysiskDokument = DefaultJsonMapper.fromJson<DokarkivGateway.Journalpost>(journalpost)
            .dokumenter!![0].dokumentvarianter[0].fysiskDokument
            .decodeToString()

        assertEquals("Hello world", fysiskDokument)
    }
}