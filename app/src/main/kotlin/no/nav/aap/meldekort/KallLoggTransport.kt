package no.nav.aap.meldekort

import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.BaseApplicationResponse
import io.ktor.server.netty.NettyApplicationCall
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2ResetFrame
import io.netty.handler.codec.http2.Http2StreamChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val confirmationTimeoutMillis = 1_000L

/**
 * Run the send unchanged, then confirm the outbound writes. Netty promise success is a local
 * transport result, not an acknowledgement from the client.
 */
internal suspend fun confirmKallLoggTransport(call: ApplicationCall, send: suspend () -> Unit): Boolean {
    // Routing wraps the engine call but shares its attributes.
    val engineCall = call as? NettyApplicationCall
        ?: call.attributes.getOrNull(BaseApplicationResponse.EngineResponseAttributeKey)?.call as? NettyApplicationCall
    val context = engineCall?.context
    val observer = if (engineCall != null && context != null && context.channel().isActive && !call.response.isCommitted) {
        val candidate = KallLoggWriteObserver(context.channel() is Http2StreamChannel) {
            // Ktor sets this lateinit property before signaling its response writer.
            try {
                engineCall.response.responseMessage
            } catch (_: UninitializedPropertyAccessException) {
                null
            }
        }
        try {
            // Ktor writes via this context, so handlers after it would not see the writes.
            context.pipeline().addBefore(context.name(), null, candidate)
            candidate
        } catch (_: RuntimeException) {
            detachObserver(context, candidate)
            null
        }
    } else {
        null
    }

    try {
        send()
        return observer != null && withTimeoutOrNull(confirmationTimeoutMillis) { observer.await() } == true
    } finally {
        if (observer != null && context != null) detachObserver(context, observer)
    }
}

private fun detachObserver(context: ChannelHandlerContext, observer: KallLoggWriteObserver) {
    val pipeline = context.pipeline()
    try {
        if (pipeline.context(observer) != null) pipeline.remove(observer)
    } catch (_: RuntimeException) {
        // Channel teardown can remove the handler or shut down its executor first.
    }
}

internal class KallLoggWriteObserver(
    private val http2: Boolean,
    private val responseHead: () -> Any?,
) : ChannelDuplexHandler() {
    private val confirmed = CompletableDeferred<Boolean>()
    // Handler methods and Ktor's write promise listeners run on the channel event loop.
    private val pending = mutableSetOf<ChannelFuture>()
    private var started = false
    private var lastSeen = false
    private val listener = ChannelFutureListener { future ->
        if (pending.remove(future)) {
            if (!future.isSuccess) finish(false)
            else if (lastSeen && pending.isEmpty()) finish(true)
        }
    }

    suspend fun await(): Boolean = confirmed.await()

    override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
        if (!confirmed.isCompleted && !lastSeen) observe(message, promise)
        context.write(message, promise)
    }

    private fun observe(message: Any, promise: ChannelPromise) {
        if (!started) {
            val finalHead = if (http2) {
                message is Http2HeadersFrame && (message.headers().status()?.toString()?.toIntOrNull() ?: 0) >= 200
            } else {
                message is HttpResponse && message.status().code() >= 200
            }
            if (!finalHead || message !== responseHead()) return
            started = true
        } else {
            // Ktor serializes HTTP/1 responses on each connection; HTTP/2 writes here belong
            // to a single stream. A new head or an unexpected frame makes attribution unsafe.
            val continuation = if (http2) {
                message is Http2DataFrame || message is Http2HeadersFrame && message.headers().status() == null
            } else {
                message is HttpContent && message !is HttpResponse
            }
            if (!continuation) {
                finish(false)
                return
            }
        }

        lastSeen = when (message) {
            is LastHttpContent -> true
            is Http2HeadersFrame -> message.isEndStream
            is Http2DataFrame -> message.isEndStream
            else -> false
        }
        if (promise.isVoid) {
            finish(false)
        } else if (pending.add(promise)) {
            promise.addListener(listener)
        }
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (http2 && message is Http2ResetFrame) finish(false)
        context.fireChannelRead(message)
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        finish(false)
        context.fireChannelInactive()
    }

    override fun handlerRemoved(context: ChannelHandlerContext) {
        finish(false)
    }

    private fun finish(success: Boolean) {
        pending.forEach { it.removeListener(listener) }
        pending.clear()
        confirmed.complete(success)
    }
}
