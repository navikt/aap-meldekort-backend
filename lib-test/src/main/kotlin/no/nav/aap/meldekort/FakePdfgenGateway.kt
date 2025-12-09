package no.nav.aap.meldekort

import no.nav.aap.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.journalføring.PdfgenGateway
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.utfylling.Utfylling
import java.time.Instant

// TODO: flytt ut til en test mappe
class FakePdfgenGateway: PdfgenGateway {
    override fun genererPdf(
        ident: Ident,
        mottatt: Instant,
        meldekort: Meldekort,
        utfylling: Utfylling,
        harBrukerVedtakIKelvin: Boolean
    ): ByteArray {
        return """
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
            .toByteArray()
    }

    companion object: Factory<FakePdfgenGateway> {
        override fun konstruer(): FakePdfgenGateway {
            return FakePdfgenGateway()
        }
    }
}
