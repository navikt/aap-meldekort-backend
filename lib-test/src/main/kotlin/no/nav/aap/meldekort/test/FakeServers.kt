package no.nav.aap.meldekort.test

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

private val logger = LoggerFactory.getLogger(FakesExtension::class.java)

interface FakeServer {
    fun setProperties(port: Int)
    val module: Application.() -> Unit
    val port: Int get() = 0
}

object FakeServers : AutoCloseable {
    private val fakeServers = listOf(FakeTokenX, FakeAzure, FakeAapApi, FakeArena, FakeDokarkiv, FakePdfgen)
        .map { it to embeddedServer(Netty, port = it.port, module = it.module) }

    private val started = AtomicBoolean(false)

    fun start() {
        if (started.get()) {
            return
        }

        for ((config, httpServer) in fakeServers) {
            httpServer.start()
            config.setProperties(httpServer.port())
        }

        setProperties()
        started.set(true)
    }


    private fun setProperties() {
        System.setProperty("nais.cluster.name", "local")
    }

    override fun close() {
        logger.info("Closing Servers.")
        if (!started.get()) {
            return
        }

        for ((_, httpServer) in fakeServers) {
            httpServer.stop(0L, 0L)
        }
    }
}

fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}