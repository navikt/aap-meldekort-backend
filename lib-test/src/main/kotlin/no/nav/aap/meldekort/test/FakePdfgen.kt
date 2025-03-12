package no.nav.aap.meldekort.test

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object FakePdfgen : FakeServer {
    override fun setProperties(port: Int) {
        System.setProperty("pdfgen.url", "http://localhost:$port")
    }

    override val module: Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            post("/api/v1/genpdf/meldekort-backend/meldekort") {
                call.respondBytes(
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
                        .toByteArray(), contentType = ContentType.Application.Pdf
                )
            }
        }
    }
}