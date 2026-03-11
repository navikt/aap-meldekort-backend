package no.nav.aap.meldekort

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.Periode
import java.time.LocalDate
import kotlin.test.assertEquals

fun assertEqualsAt(expected: Any?, json: JsonNode, path: String) {
    val expectedNormalized = when {
        expected is LocalDate -> expected.toString()
        else -> expected
    }
    val obj = json.at(path)
    val result = when {
        obj.isNull -> null
        obj.isTextual -> obj.asText()
        obj.isInt -> obj.asInt()
        expectedNormalized is Periode ->
            Periode(LocalDate.parse(obj["fom"].asText()), LocalDate.parse(obj["tom"].asText()))

        else -> error("uforventet type ${obj.javaClass.name}")
    }

    assertEquals(expectedNormalized, result)
}
