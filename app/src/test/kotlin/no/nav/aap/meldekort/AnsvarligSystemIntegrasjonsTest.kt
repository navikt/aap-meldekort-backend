package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.Periode
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class AnsvarligSystemIntegrasjonsTest {
    private val idag = LocalDate.now()
    @AutoClose
    private val app = AppInstance(idag)

    @Test
    fun `ansvarlig system, ingen sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i kelvin`() {
        val fnr = fødselsnummerGenerator.next()
        app.kelvinSak(fnr)
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, sak kun i arena`() {
        val fnr = fødselsnummerGenerator.next()
        app.arenaSak(fnr)

        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i kelvin`() {
        val fnr = fødselsnummerGenerator.next()

        app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))
        app.kelvinSak(fnr)

        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("AAP", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }

    @Test
    fun `ansvarlig system, saker både i kelvin og arena, nyeste sak i arena`() {
        val fnr = fødselsnummerGenerator.next()

        app.arenaSak(fnr, rettighetsperiode = Periode(idag.minusMonths(1), idag.plusMonths(1)))
        app.kelvinSak(fnr, rettighetsperiode = Periode(idag.minusMonths(20), idag.minusMonths(18)))

        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system")?.asText())
        assertEquals("FELLES", app.get<JsonNode>(fnr, "/api/ansvarlig-system-felles")?.asText())
    }
}
