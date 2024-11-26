package no.nav.aap.meldekort.test

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class FakesExtension() : BeforeAllCallback,
    BeforeEachCallback {

    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    }

    override fun beforeAll(context: ExtensionContext?) {
        FakeServers.start()
    }

    override fun beforeEach(context: ExtensionContext?) {
    }

}