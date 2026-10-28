package com.myiptv.clean.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.myiptv.clean.ui.compose.MediaPlaybackType

private const val MEDIA_SOURCE_PREFERENCES = "media_sources"

data class ConfiguredMediaSource(
    val url: String,
    val type: MediaPlaybackType
)

object LocalMediaSources {
    fun get(context: Context, mediaId: String): ConfiguredMediaSource? {
        val preferences = context.applicationContext.getSharedPreferences(MEDIA_SOURCE_PREFERENCES, Context.MODE_PRIVATE)
        val key = encodedKey(mediaId)
        val url = preferences.getString("$key.url", null)?.takeIf(String::isNotBlank) ?: return null
        val type = runCatching {
            MediaPlaybackType.valueOf(preferences.getString("$key.type", MediaPlaybackType.DIRECT.name).orEmpty())
        }.getOrDefault(MediaPlaybackType.DIRECT)
        return ConfiguredMediaSource(url, type)
    }

    fun save(
        context: Context,
        mediaId: String,
        url: String,
        type: MediaPlaybackType
    ): Boolean {
        val cleanUrl = url.trim()
        if (!isAllowedSource(cleanUrl, type)) return false
        val key = encodedKey(mediaId)
        context.applicationContext.getSharedPreferences(MEDIA_SOURCE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString("$key.url", cleanUrl)
            .putString("$key.type", type.name)
            .apply()
        return true
    }

    fun remove(context: Context, mediaId: String) {
        val key = encodedKey(mediaId)
        context.applicationContext.getSharedPreferences(MEDIA_SOURCE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove("$key.url")
            .remove("$key.type")
            .apply()
    }

    private fun isAllowedSource(value: String, type: MediaPlaybackType): Boolean {
        val candidate = if (type == MediaPlaybackType.IFRAME && value.contains("<iframe", ignoreCase = true)) {
            extractIframeUrl(value)
        } else value
        return candidate?.let(::isHttpUrl) == true
    }

    private fun extractIframeUrl(markup: String): String? {
        val sourceStart = markup.indexOf("src", ignoreCase = true)
        if (sourceStart < 0) return null
        val equals = markup.indexOf('=', startIndex = sourceStart + 3)
        if (equals < 0) return null
        var start = equals + 1
        while (start < markup.length && markup[start].isWhitespace()) start++
        if (start >= markup.length) return null
        val quote = markup[start].takeIf { it == '"' || it == '\'' }
        if (quote != null) start++
        val end = if (quote != null) markup.indexOf(quote, start) else markup.indexOfAny(charArrayOf(' ', '\t', '\r', '\n', '>'), start)
        val safeEnd = if (end < 0) markup.length else end
        return markup.substring(start, safeEnd).takeIf(String::isNotBlank)
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = Uri.parse(value.trim())
        uri.scheme?.lowercase() in setOf("https", "http") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun encodedKey(mediaId: String): String =
        Base64.encodeToString(mediaId.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}
