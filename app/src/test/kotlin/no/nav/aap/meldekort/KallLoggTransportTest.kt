package no.nav.aap.meldekort

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.util.ReferenceCountUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Timeout(5)
class KallLoggTransportTest {
    @Test
    fun `successful last write cannot conceal a late failure of an earlier write`() = runBlocking {
        val head = http1Head()
        withChannel(head) { channel, observer ->
            val confirmed = async(Dispatchers.Unconfined) { observer.await() }
            val header = channel.writeWithPromise(head)
            val body = channel.writeWithPromise(DefaultHttpContent(Unpooled.wrappedBuffer(byteArrayOf(1))))
            val end = channel.writeWithPromise(LastHttpContent.EMPTY_LAST_CONTENT)

            header.setSuccess()
            end.setSuccess()
            assertFalse(confirmed.isCompleted)
            body.setFailure(IllegalStateException("transport write failed"))
            assertFalse(confirmed.await())
        }
    }

    @Test
    fun `only the selected response head can start HTTP1 confirmation`() = runBlocking {
        val head = http1Head()
        withChannel(head) { channel, observer ->
            val confirmed = async(Dispatchers.Unconfined) { observer.await() }
            channel.writeWithPromise(http1Head()).setSuccess()
            channel.writeWithPromise(LastHttpContent.EMPTY_LAST_CONTENT).setSuccess()
            assertFalse(confirmed.isCompleted)

            channel.writeWithPromise(head).setSuccess()
            assertFalse(confirmed.isCompleted)
            channel.writeWithPromise(LastHttpContent.EMPTY_LAST_CONTENT).setSuccess()
            assertTrue(confirmed.await())
        }
    }

    @Test
    fun `a failed full HTTP1 response write is not confirmed`() = runBlocking {
        val head = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        withChannel(head) { channel, observer ->
            channel.writeWithPromise(head).setFailure(IllegalStateException("transport write failed"))
            assertFalse(observer.await())
        }
    }

    @Test
    fun `HTTP2 confirmation waits for end stream and all write promises`() = runBlocking {
        val head = DefaultHttp2HeadersFrame(DefaultHttp2Headers().status("200"), false)
        withChannel(head, http2 = true) { channel, observer ->
            val confirmed = async(Dispatchers.Unconfined) { observer.await() }
            channel.writeWithPromise(head).setSuccess()
            val data = channel.writeWithPromise(DefaultHttp2DataFrame(false))
            assertFalse(confirmed.isCompleted)
            channel.writeWithPromise(DefaultHttp2HeadersFrame(DefaultHttp2Headers().add("test", "trailer"), true))
                .setSuccess()
            assertFalse(confirmed.isCompleted)
            data.setSuccess()
            assertTrue(confirmed.await())
        }
    }

    private fun http1Head() = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)

    private suspend fun withChannel(
        head: Any,
        http2: Boolean = false,
        test: suspend (EmbeddedChannel, KallLoggWriteObserver) -> Unit,
    ) {
        val observer = KallLoggWriteObserver(http2) { head }
        val channel = EmbeddedChannel(HoldWrites(), observer)
        try {
            test(channel, observer)
        } finally {
            channel.finishAndReleaseAll()
        }
    }

    private class HoldWrites : ChannelOutboundHandlerAdapter() {
        override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
            // Simulate writes whose transport result is only known when the test settles the original promise.
            ReferenceCountUtil.release(message)
        }
    }

    private fun EmbeddedChannel.writeWithPromise(message: Any): ChannelPromise =
        newPromise().also { write(message, it) }
}
