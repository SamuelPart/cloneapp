package com.myiptv.clean.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class RemoteCatalogClientTest {
    private val valid = "#EXTM3U\n#EXTINF:-1 group-title=\"Peliculas\",Real\nhttps://video.example/watch/1\n"

    private class Response(
        url: URL,
        private val code: Int = 200,
        private val body: ByteArray = byteArrayOf(),
        private val headers: Map<String, String> = emptyMap()
    ) : HttpURLConnection(url) {
        override fun getResponseCode() = code
        override fun getHeaderField(name: String): String? = headers[name]
        override fun getInputStream() = ByteArrayInputStream(body)
        override fun disconnect() = Unit
        override fun connect() = Unit
        override fun usingProxy() = false
    }

    private fun expectFailure(block: () -> Unit) {
        try { block(); fail("Expected an IOException") } catch (_: IOException) { }
    }

    @Test fun fallsBackWhenTheFirstHostFailsOrReturnsInvalidContent() {
        val calls = mutableListOf<String>()
        val client = RemoteCatalogClient(
            endpoints = listOf("https://raw.githubusercontent.com/catalog", "https://api.github.com/catalog"),
            openConnection = { url ->
                calls += url.host
                if (url.host == "raw.githubusercontent.com") Response(url, body = "<html>error</html>".toByteArray())
                else Response(url, body = valid.toByteArray())
            }
        )
        assertEquals(valid, client.fetch())
        assertEquals(listOf("raw.githubusercontent.com", "api.github.com"), calls)
    }

    @Test fun rejectsRedirectsToUntrustedOrNonHttpsHosts() {
        for (destination in listOf("https://evil.example/catalog", "http://api.github.com/catalog", "https://user@api.github.com/catalog")) {
            val calls = mutableListOf<String>()
            val client = RemoteCatalogClient(listOf("https://api.github.com/catalog"), openConnection = { url ->
                calls += url.host
                Response(url, 302, headers = mapOf("Location" to destination))
            })
            expectFailure { client.fetch() }
            assertEquals(listOf("api.github.com"), calls)
        }
    }

    @Test fun acceptsOnlyLimitedValidUtf8ResponsesWithCompleteLength() {
        val endpoint = listOf("https://api.github.com/catalog")
        fun client(bytes: ByteArray, headers: Map<String, String> = emptyMap()) =
            RemoteCatalogClient(endpoint, openConnection = { url -> Response(url, body = bytes, headers = headers) })
        val bytes = valid.toByteArray()
        assertEquals(valid, client(bytes, mapOf("Content-Length" to bytes.size.toString())).fetch())
        expectFailure { client(bytes, mapOf("Content-Length" to "9999")).fetch() }
        expectFailure { client(bytes, mapOf("Content-Length" to (RemoteCatalogClient.MAX_CATALOG_BYTES + 1).toString())).fetch() }
        expectFailure { client(ByteArray(RemoteCatalogClient.MAX_CATALOG_BYTES + 1)).fetch() }
        expectFailure { client(byteArrayOf(0xC3.toByte(), 0x28)).fetch() }
        expectFailure { client("#EXTM3U\n#EXTINF:-1 group-title=\"Peliculas\",Rota\n".toByteArray()).fetch() }
    }

    @Test fun followsAllowedRedirectsButStopsLoops() {
        val calls = mutableListOf<String>()
        val client = RemoteCatalogClient(listOf("https://github.com/catalog"), openConnection = { url ->
            calls += url.host
            if (url.host == "github.com") Response(url, 302, headers = mapOf("Location" to "https://raw.githubusercontent.com/catalog"))
            else Response(url, body = valid.toByteArray())
        })
        assertEquals(valid, client.fetch())
        assertEquals(listOf("github.com", "raw.githubusercontent.com"), calls)
        val looping = RemoteCatalogClient(listOf("https://github.com/catalog"), openConnection = { url ->
            Response(url, 302, headers = mapOf("Location" to "/catalog"))
        })
        expectFailure { looping.fetch() }
        assertTrue(calls.size == 2)
    }
}
