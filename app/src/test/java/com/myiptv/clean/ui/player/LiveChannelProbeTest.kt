package com.myiptv.clean.ui.player

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URL
import kotlin.concurrent.thread

class LiveChannelProbeTest {
    private class FakeConnection(url: URL, private val status: Int, private val location: String? = null) : HttpURLConnection(url) {
        val headers = mutableMapOf<String, String>()
        var closed = false
        var readBody = false
        var timeout = false
        override fun connect() = Unit
        override fun usingProxy() = false
        override fun disconnect() { closed = true }
        override fun setRequestProperty(key: String, value: String) { headers[key] = value }
        override fun getResponseCode(): Int {
            if (timeout) throw SocketTimeoutException("slow server")
            return status
        }
        override fun getHeaderField(name: String): String? = if (name == "Location") location else null
        override fun getInputStream(): ByteArrayInputStream {
            readBody = true
            return ByteArrayInputStream(ByteArray(0))
        }
    }

    @Test fun readsOnlyHeadersAndUsesTheSameSafeUserAgentAndReferrerAsVlc() {
        val connection = FakeConnection(URL("https://example.test/live"), 200)
        val result = probeChannelHeaders("https://example.test/live", " Box Player ", "https://origin.test/", { connection })
        assertEquals(ChannelProbeResult.RESPONDING, result)
        assertEquals("Box Player", connection.headers["User-Agent"])
        assertEquals("https://origin.test/", connection.headers["Referer"])
        assertEquals("bytes=0-0", connection.headers["Range"])
        assertTrue(connection.closed)
        assertFalse(connection.readBody)
    }

    @Test fun blockedProbeAndTimeoutAreNotMistakenForADeadStream() {
        val forbidden = FakeConnection(URL("https://example.test/live"), 403)
        assertEquals(ChannelProbeResult.INCONCLUSIVE, probeChannelHeaders(forbidden.url.toString(), null, "bad\nheader", { forbidden }))
        assertEquals("SamPlay/1.0", forbidden.headers["User-Agent"])
        assertFalse(forbidden.headers.containsKey("Referer"))
        val missing = FakeConnection(URL("https://example.test/live"), 404)
        assertEquals(ChannelProbeResult.UNAVAILABLE, probeChannelHeaders(missing.url.toString(), null, null, { missing }))
        val slow = FakeConnection(URL("https://example.test/live"), 200).apply { timeout = true }
        assertEquals(ChannelProbeResult.INCONCLUSIVE, probeChannelHeaders(slow.url.toString(), null, null, { slow }))
        assertTrue(slow.closed)
        assertEquals(ChannelProbeResult.INCONCLUSIVE, probeChannelHeaders("file:///etc/passwd", null, null))
    }

    @Test fun followsBoundedHttpRedirectsAndKeepsChannelHeaders() {
        val visited = mutableListOf<FakeConnection>()
        val result = probeChannelHeaders("https://example.test/live", "TV", "https://origin.test/", { url ->
            FakeConnection(url, if (visited.isEmpty()) 302 else 206, if (visited.isEmpty()) "/new" else null).also(visited::add)
        })
        assertEquals(ChannelProbeResult.RESPONDING, result)
        assertEquals(listOf("/live", "/new"), visited.map { it.url.path })
        assertTrue(visited.all { it.closed && it.headers["Referer"] == "https://origin.test/" })
    }

    @Test fun aSlowRealServerReturnsInconclusiveWithoutBlockingPlaybackIndefinitely() = runBlocking {
        ServerSocket(0).use { server ->
            val worker = thread(isDaemon = true) {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (true) if (reader.readLine().isNullOrEmpty()) break
                    Thread.sleep(2_000) // The server has not declared the channel dead.
                }
            }
            val started = System.nanoTime()
            val result = LiveChannelProbe.check("http://127.0.0.1:${server.localPort}/slow", null, null)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertEquals(ChannelProbeResult.INCONCLUSIVE, result)
            assertTrue("La comprobación debe estar limitada", elapsedMs < 5_000)
            worker.join(3_000)
        }
    }

    @Test fun asynchronousProbeCanContactARealLocalHttpServer() = runBlocking {
        ServerSocket(0).use { server ->
            val worker = thread(isDaemon = true) {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (true) if (reader.readLine().isNullOrEmpty()) break
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    socket.getOutputStream().flush()
                }
            }
            val result = LiveChannelProbe.check("http://127.0.0.1:${server.localPort}/live", "RealTV", null)
            assertEquals(ChannelProbeResult.RESPONDING, result)
            worker.join(2_000)
            assertFalse(worker.isAlive)
        }
    }
}
