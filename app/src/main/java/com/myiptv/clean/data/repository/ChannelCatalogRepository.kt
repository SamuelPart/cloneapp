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
    var statusMessage: String = "Preparando canales en directo."
        private set

    suspend fun refresh(force: Boolean = false): List<LiveChannel> {
        refreshMutex.lock()
        try {
            val now = System.currentTimeMillis()
            if (!force && channelCache.isNotEmpty() && now - lastRefreshAt < CACHE_DURATION_MS) {
                return channelCache
            }

            var hadUpdateFailure = false
            val tdtChannels = try {
                TdtChannelsRepository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                hadUpdateFailure = true
                TdtChannelsRepository.cachedChannels()
            }
            val iptvOrgChannels = try {
                IptvOrgRepository.refresh(force)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                hadUpdateFailure = true
                IptvOrgRepository.cachedChannels()
            }
            if (IptvOrgRepository.lastErrorMessage.isNotBlank()) hadUpdateFailure = true

            val combined = (tdtChannels + iptvOrgChannels).distinctBy(LiveChannel::id)
            if (combined.isEmpty()) {
                statusMessage = "No se pudieron cargar canales. Comprueba la conexión e inténtalo de nuevo."
                throw IOException(statusMessage)
            }

            channelCache = combined
            lastRefreshAt = now
            statusMessage = if (hadUpdateFailure) {
                "No se pudieron actualizar todos los canales; se muestran los disponibles."
            } else {
                "Canales en directo actualizados."
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
