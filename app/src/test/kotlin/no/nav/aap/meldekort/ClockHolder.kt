package no.nav.aap.meldekort

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ClockHolder(var idag: LocalDate) : Clock() {
    override fun getZone(): ZoneId? {
        return ZoneId.of("Europe/Oslo")
    }

    override fun withZone(zone: ZoneId?): Clock {
        TODO()
    }

    override fun instant(): Instant? {
        return idag.atTime(10, 10).atZone(ZoneId.of("Europe/Oslo")).toInstant()
    }
}