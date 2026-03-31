package no.nav.aap.meldekort.test

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean


interface FakeServer {
    fun setProperties(port: Int)
    val module: Application.() -> Unit
    val port: Int get() = 0
}

object FakeServers : AutoCloseable {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val fakeServers = listOf(FakeTokenX, FakeAzure, FakeAapApi, FakeArena, FakeDokarkiv, FakePdfgen)
        .map { it to embeddedServer(Netty, port = it.port, module = it.module) }

    init {
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        for ((config, httpServer) in fakeServers) {
            httpServer.start()
            config.setProperties(httpServer.port())
        }

        setProperties()
    }


    private fun setProperties() {
        System.setProperty("nais.cluster.name", "local")
    }

    override fun close() {
        logger.info("Closing Servers.")
        if (!started.compareAndSet(true, false)) {
            return
        }

        for ((_, httpServer) in fakeServers) {
            httpServer.stop(1_000L, 5_000L)
        }
    }

    @Suppress("PropertyName")
    data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )

}

fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}