package com.myiptv.clean.data.repository

import com.myiptv.clean.data.api.TdtChannelsApi
import com.myiptv.clean.ui.compose.LiveChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object TdtChannelsRepository {
    private const val BASE_URL = "https://www.tdtchannels.com/"
    private val colors = listOf(0xFF4E86B9L, 0xFF8B62C2L, 0xFF3B9B83L, 0xFFCC7651L, 0xFFBD5272L)

    private val api: TdtChannelsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .build()
            .create(TdtChannelsApi::class.java)
    }

    @Volatile
    private var channelCache: List<LiveChannel> = emptyList()
    private val channelsById = ConcurrentHashMap<String, LiveChannel>()

    suspend fun refresh(): List<LiveChannel> {
        val playlist = api.televisionPlaylist().use { it.string() }
        val parsed = withContext(Dispatchers.Default) { parsePlaylist(playlist) }
        if (parsed.isEmpty()) throw IOException("La lista M3U de TDTChannels no contiene canales reproducibles.")
        channelsById.clear()
        channelsById.putAll(parsed.associateBy(LiveChannel::id))
        channelCache = parsed
        return parsed
    }

    fun cachedChannels(): List<LiveChannel> = channelCache

    fun cachedChannel(id: String): LiveChannel? = channelsById[id]

    internal fun parsePlaylist(playlist: String): List<LiveChannel> {
        val channels = LinkedHashMap<String, LiveChannel>()
        var pending: ChannelMetadata? = null
        var currentGroup = ""

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val comma = line.indexOf(',')
                    if (comma > 0) {
                        val attributes = line.substring(0, comma)
                        val name = line.substring(comma + 1).trim()
                        pending = if (name.isNotBlank()) {
                            ChannelMetadata(
                                name = name,
                                epgId = attribute(attributes, "tvg-id"),
                                logoUrl = attribute(attributes, "tvg-logo"),
                                category = attribute(attributes, "group-title").ifBlank { currentGroup }
                            )
                        } else null
                    } else {
                        pending = null
                    }
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    currentGroup = line.substringAfter(':').trim()
                }
                line.startsWith('#') || line.isBlank() -> Unit
                line.startsWith("https://", ignoreCase = true) || line.startsWith("http://", ignoreCase = true) -> {
                    val metadata = pending
                    if (metadata != null) {
                        val stableIdentity = metadata.epgId.ifBlank { metadata.name }
                        val id = "tdt_${Integer.toHexString(stableIdentity.hashCode())}"
                        val letters = metadata.name.split(Regex("\\s+")).filter(String::isNotBlank).take(2)
                            .mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()
                        val accent = colors[metadata.name.hashCode().ushr(1) % colors.size]
                        channels.putIfAbsent(
                            id,
                            LiveChannel(
                                id = id,
                                name = metadata.name,
                                category = metadata.category.ifBlank { "Sin categoría" },
                                logoLetters = letters.ifBlank { metadata.name.take(2).uppercase() },
                                streamUrl = line,
                                accent = accent,
                                logoUrl = metadata.logoUrl.takeIf(String::isNotBlank),
                                epgId = metadata.epgId.takeIf(String::isNotBlank)
                            )
                        )
                    }
                    pending = null
                }
            }
        }

        return channels.values.toList()
    }

    private fun attribute(source: String, name: String): String {
        val pattern = Regex("(?:^|\\s)${Regex.escape(name)}=(?:\"([^\"]*)\"|'([^']*)'|([^\\s]*))")
        val match = pattern.find(source) ?: return ""
        return match.groupValues.drop(1).firstOrNull(String::isNotEmpty).orEmpty()
    }

    private data class ChannelMetadata(
        val name: String,
        val epgId: String,
        val logoUrl: String,
        val category: String
    )
}
