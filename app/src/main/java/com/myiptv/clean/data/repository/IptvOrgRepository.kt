package com.myiptv.clean.data.repository

import android.net.Uri
import com.myiptv.clean.data.api.IptvOrgApi
import com.myiptv.clean.data.model.IptvOrgChannel
import com.myiptv.clean.data.model.IptvOrgStream
import com.myiptv.clean.ui.compose.LiveChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object IptvOrgRepository {
    private const val BASE_URL = "https://iptv-org.github.io/"
    private const val COUNTRY_CODE = "PE"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L
    private val colors = listOf(0xFF4E86B9L, 0xFF8B62C2L, 0xFF3B9B83L, 0xFFCC7651L, 0xFFBD5272L)
    private val refreshMutex = Mutex()

    private val api: IptvOrgApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IptvOrgApi::class.java)
    }

    @Volatile
    private var channelCache: List<LiveChannel> = emptyList()
    @Volatile
    private var lastRefreshAt: Long = 0L
    @Volatile
    var lastErrorMessage: String = ""
        private set

    suspend fun refresh(force: Boolean = false): List<LiveChannel> {
        refreshMutex.lock()
        try {
            val now = System.currentTimeMillis()
            if (!force && channelCache.isNotEmpty() && now - lastRefreshAt < CACHE_DURATION_MS) {
                return channelCache
            }

            return try {
                val parsed = coroutineScope {
                    val channelsRequest = async { api.channels() }
                    val streamsRequest = async { api.streams() }
                    val channelsById = channelsRequest.await()
                        .asSequence()
                        .filter { channel ->
                            channel.country?.equals(COUNTRY_CODE, ignoreCase = true) == true &&
                                channel.isNsfw == false && channel.closed.isNullOrBlank() && !channel.name.isNullOrBlank()
                        }
                        .mapNotNull { channel -> channel.id?.let { it to channel } }
                        .toMap()
                    streamsRequest.await().mapNotNull { stream -> stream.toLiveChannel(channelsById) }
                }.distinctBy(LiveChannel::id)

                if (parsed.isEmpty()) throw IOException("IPTV-org no devolvió emisiones públicas elegibles para Perú.")
                channelCache = parsed
                lastRefreshAt = now
                lastErrorMessage = ""
                parsed
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastErrorMessage = "No se pudo actualizar IPTV-org: ${error.message.orEmpty()}".trim()
                if (channelCache.isNotEmpty()) channelCache else throw error
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    fun cachedChannels(): List<LiveChannel> = channelCache

    private fun IptvOrgStream.toLiveChannel(channelsById: Map<String, IptvOrgChannel>): LiveChannel? {
        val channelId = channel?.takeIf(String::isNotBlank) ?: return null
        val metadata = channelsById[channelId] ?: return null
        if (labels.orEmpty().any { it.equals("Geo-blocked", ignoreCase = true) }) return null

        val cleanUrl = url?.trim()?.takeIf(String::isNotBlank) ?: return null
        val uri = runCatching { Uri.parse(cleanUrl) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null

        val displayName = title?.takeIf(String::isNotBlank) ?: metadata.name?.takeIf(String::isNotBlank) ?: return null
        val identity = "$channelId|${feed.orEmpty()}|$cleanUrl"
        val id = "iptv_${Integer.toHexString(identity.hashCode())}"
        val letters = displayName.split(Regex("\\s+")).take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("").ifBlank { "TV" }
        val category = metadata.categories.orEmpty().firstOrNull()?.takeIf(String::isNotBlank)
            ?.replaceFirstChar(Char::uppercase) ?: "IPTV-org"
        val accent = colors[displayName.hashCode().ushr(1) % colors.size]

        return LiveChannel(
            id = id,
            name = displayName,
            category = category,
            logoLetters = letters,
            streamUrl = cleanUrl,
            accent = accent,
            epgId = channelId,
            sourceName = "IPTV-org",
            userAgent = userAgent.safeHeaderValue(),
            referrer = referrer.safeHttpUrl()
        )
    }

    private fun String?.safeHeaderValue(): String? = this?.trim()?.takeIf { value ->
        value.isNotBlank() && value.length <= 512 && '\r' !in value && '\n' !in value
    }

    private fun String?.safeHttpUrl(): String? = this?.trim()?.takeIf { value ->
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return@takeIf false
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && '\r' !in value && '\n' !in value
    }
}
