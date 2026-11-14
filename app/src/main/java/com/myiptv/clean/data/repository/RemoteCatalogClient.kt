package com.myiptv.clean.data.repository

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Public, owner-maintained playlist. The API URL is a fallback if the raw CDN is unreachable. */
internal class RemoteCatalogClient(
    private val endpoints: List<String> = listOf(RAW_CATALOG_URL, API_CATALOG_URL),
    private val allowedHosts: Set<String> = setOf("raw.githubusercontent.com", "api.github.com", "github.com"),
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 12_000,
    private val openConnection: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as? HttpURLConnection ?: throw IOException("No se pudo abrir la conexión del catálogo")
    }
) {
    fun fetch(): String {
        var failure: IOException? = null
        for (endpoint in endpoints) {
            try {
                val text = download(URL(endpoint))
                if (parseRemoteCatalog(text) == null) throw IOException("El catálogo remoto no es un M3U válido")
                return text
            } catch (error: IOException) {
                failure = error
            } catch (error: SecurityException) {
                failure = IOException("No se pudo acceder al catálogo remoto", error)
            }
        }
        throw failure ?: IOException("No hay una dirección de catálogo configurada")
    }

    private fun download(initialUrl: URL): String {
        var url = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            if (url.protocol != "https" || url.host.lowercase(Locale.ROOT) !in allowedHosts || url.userInfo != null) {
                throw IOException("Dirección de catálogo no permitida")
            }
            val connection = openConnection(url)
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = connectTimeoutMs
                connection.readTimeout = readTimeoutMs
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", "SamPlay-Catalog/1.0")
                connection.setRequestProperty("Accept", "application/vnd.github.raw+json, text/plain, */*")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("Cache-Control", "no-cache")
                val response = connection.responseCode
                if (response in REDIRECT_CODES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Demasiadas redirecciones")
                    val location = connection.getHeaderField("Location") ?: throw IOException("Redirección sin destino")
                    url = URL(url, location)
                    return@repeat
                }
                if (response != HttpURLConnection.HTTP_OK) throw IOException("Catálogo no disponible (HTTP $response)")
                val declaredLength = connection.getHeaderField("Content-Length")?.toLongOrNull()
                if (declaredLength != null && declaredLength > MAX_CATALOG_BYTES) throw IOException("Catálogo demasiado grande")
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        total += count
                        if (total > MAX_CATALOG_BYTES) throw IOException("Catálogo demasiado grande")
                        output.write(buffer, 0, count)
                    }
                    if (declaredLength != null && declaredLength != total.toLong()) {
                        throw IOException("Descarga incompleta del catálogo")
                    }
                    output.toByteArray()
                }
                return Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString()
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("No se pudo descargar el catálogo")
    }

    companion object {
        const val RAW_CATALOG_URL = "https://raw.githubusercontent.com/SamuelPart/cloneapp/arena/2b6cd77d-cloneapp/app/src/main/assets/samplay_catalog.m3u"
        const val API_CATALOG_URL = "https://api.github.com/repos/SamuelPart/cloneapp/contents/app/src/main/assets/samplay_catalog.m3u?ref=arena%2F2b6cd77d-cloneapp"
        const val MAX_CATALOG_BYTES = 4 * 1024 * 1024
        private const val MAX_REDIRECTS = 3
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
