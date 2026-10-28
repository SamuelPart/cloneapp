package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.LiveChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.io.IOException

object ChannelCatalogRepository {
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L
    private val refreshMutex = Mutex()

    @Volatile
    private var channelCache: List<LiveChannel> = emptyList()
    @Volatile
    private var lastRefreshAt: Long = 0L
    @Volatile
    var statusMessage: String = "Fuentes de canales: TDTChannels e IPTV-org (Perú)."
        private set

    suspend fun refresh(force: Boolean = false): List<LiveChannel> {
        refreshMutex.lock()
        try {
            val now = System.currentTimeMillis()
            if (!force && channelCache.isNotEmpty() && now - lastRefreshAt < CACHE_DURATION_MS) {
                return channelCache
            }

            val errors = mutableListOf<String>()
            val tdtChannels = try {
                TdtChannelsRepository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errors += "TDTChannels"
                TdtChannelsRepository.cachedChannels()
            }
            val iptvOrgChannels = try {
                IptvOrgRepository.refresh(force)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errors += "IPTV-org"
                IptvOrgRepository.cachedChannels()
            }
            if (IptvOrgRepository.lastErrorMessage.isNotBlank() && "IPTV-org" !in errors) {
                errors += "IPTV-org"
            }

            val combined = (tdtChannels + iptvOrgChannels).distinctBy(LiveChannel::id)
            if (combined.isEmpty()) {
                statusMessage = "No se pudo cargar ningún canal desde TDTChannels ni IPTV-org."
                throw IOException(statusMessage)
            }

            channelCache = combined
            lastRefreshAt = now
            statusMessage = if (errors.isEmpty()) {
                "Fuentes activas: TDTChannels e IPTV-org (Perú)."
            } else {
                "Carga parcial; no se pudo actualizar: ${errors.distinct().joinToString(" y ")}."
            }
            return combined
        } finally {
            refreshMutex.unlock()
        }
    }

    fun cachedChannels(): List<LiveChannel> = channelCache.ifEmpty {
        (TdtChannelsRepository.cachedChannels() + IptvOrgRepository.cachedChannels()).distinctBy(LiveChannel::id)
    }

    fun cachedChannel(id: String): LiveChannel? =
        channelCache.firstOrNull { it.id == id }
            ?: TdtChannelsRepository.cachedChannel(id)
            ?: IptvOrgRepository.cachedChannels().firstOrNull { it.id == id }
}
