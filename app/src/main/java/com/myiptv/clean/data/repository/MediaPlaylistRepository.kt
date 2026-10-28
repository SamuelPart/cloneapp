package com.myiptv.clean.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object MediaPlaylistRepository {
    const val ASSET_NAME = "samplay_catalog.m3u"

    suspend fun load(context: Context): MediaPlaylist = withContext(Dispatchers.IO) {
        try {
            context.applicationContext.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
                MediaPlaylistParser.parse(reader.readText())
            }
        } catch (_: IOException) {
            MediaPlaylist(errorMessage = "No se pudo leer el catálogo M3U incluido en la aplicación.")
        }
    }
}
