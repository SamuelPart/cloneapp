package com.myiptv.clean.data.repository

import android.content.Context
import android.util.AtomicFile
import com.myiptv.clean.ui.compose.MediaKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** One atomic file contains both the last good M3U and notifications still awaiting permission. */
internal class CatalogFileStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir, "samplay_catalog_cache.json"))

    fun read(): StoredCatalog? {
        return try {
            if (file.baseFile.length() > MAX_CACHE_BYTES) return null
            val text = file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            if (json.optInt("version") != 1) return null
            val content = json.getString("content")
            if (content.toByteArray(Charsets.UTF_8).size > RemoteCatalogClient.MAX_CATALOG_BYTES || parseRemoteCatalog(content) == null) return null
            val array = json.optJSONArray("pending") ?: JSONArray()
            val pending = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val title = item.optString("title").takeIf(String::isNotBlank) ?: continue
                    val kind = runCatching { MediaKind.valueOf(item.optString("kind")) }.getOrNull() ?: continue
                    val season = if (item.has("season") && !item.isNull("season")) item.optInt("season", -1) else null
                    if (season != null && season < 0) continue
                    val episodeArray = item.optJSONArray("episodes") ?: JSONArray()
                    val episodes = (0 until episodeArray.length()).map { episodeArray.optInt(it, -1) }
                        .filter { it >= 0 }.distinct().sorted()
                    if (season != null && episodes.isEmpty()) continue
                    add(CatalogAddition(id, title, kind, season, episodes))
                }
            }
            StoredCatalog(content, pending)
        } catch (_: Exception) {
            // A missing/interrupted/invalid cache falls back to the packaged asset.
            null
        }
    }

    @Throws(IOException::class)
    fun write(catalog: StoredCatalog) {
        val pending = JSONArray()
        catalog.pending.forEach { addition ->
            val item = JSONObject()
                .put("id", addition.mediaId)
                .put("title", addition.title)
                .put("kind", addition.kind.name)
            if (addition.season != null) {
                item.put("season", addition.season)
                item.put("episodes", JSONArray(addition.episodes))
            }
            pending.put(item)
        }
        val bytes = JSONObject().put("version", 1).put("content", catalog.content)
            .put("pending", pending).toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_CACHE_BYTES) throw IOException("No se pudo guardar el catálogo: tamaño excesivo")
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw IOException("No se pudo guardar el catálogo", error)
        }
    }

    private companion object {
        const val MAX_CACHE_BYTES = 16 * 1024 * 1024
    }
}
