package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/** Load metadata progressively with bounded concurrency. An API failure never removes M3U links. */
internal suspend fun loadPlaylistMetadata(
    playlist: MediaPlaylist,
    resolve: suspend (MediaItem) -> MediaItem?,
    onUpdate: (MediaPlaylist) -> Unit
) = coroutineScope {
    val next = AtomicInteger(0)
    val mutex = Mutex()
    var current = playlist
    repeat(minOf(3, playlist.items.size)) {
        launch {
            while (true) {
                val index = next.getAndIncrement()
                val original = playlist.items.getOrNull(index) ?: break
                val enriched = try {
                    resolve(original)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null // Other titles and their sources stay available; opening this item can retry.
                }
                if (enriched != null && enriched.id == original.id && enriched.kind == original.kind) {
                    mutex.withLock {
                        current = current.copy(items = current.items.map { if (it.id == original.id) enriched else it })
                        onUpdate(current)
                    }
                }
            }
        }
    }
}
