package com.myiptv.clean.data.repository

import android.content.Context
import com.myiptv.clean.notifications.CatalogNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** Cache, notifications and the visible playlist share one lock, including background job updates. */
object MediaPlaylistRepository {
    const val ASSET_NAME = "samplay_catalog.m3u"
    const val REFRESH_INTERVAL_MS = 15 * 60 * 1_000L

    private val stateLock = Mutex()
    private val refreshLock = Mutex()
    private val playlists = MutableStateFlow<MediaPlaylist?>(null)
    private val mutableSyncStatus = MutableStateFlow("Catálogo incluido en la aplicación.")
    val syncStatus = mutableSyncStatus.asStateFlow()
    private var initialized = false
    private var snapshot: StoredCatalog? = null

    fun observe(context: Context): Flow<MediaPlaylist> = flow {
        initialize(context)
        emitAll(playlists.filterNotNull())
    }

    suspend fun load(context: Context): MediaPlaylist {
        initialize(context)
        return playlists.value ?: MediaPlaylist.Loading
    }

    private suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        stateLock.withLock initializeLock@ {
            if (initialized) return@initializeLock
            val appContext = context.applicationContext
            val cached = CatalogFileStore(appContext).read()
            val bundled = if (cached == null) {
                try {
                    val text = appContext.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    text.takeIf { parseRemoteCatalog(it) != null }?.let(::StoredCatalog)
                } catch (_: IOException) {
                    null
                }
            } else null
            snapshot = cached ?: bundled
            playlists.value = snapshot?.content?.let(::parseRemoteCatalog)
                ?: MediaPlaylist(errorMessage = "No se pudo leer el catálogo M3U incluido en la aplicación.")
            mutableSyncStatus.value = if (cached != null) "Mostrando la última copia guardada." else "Mostrando el catálogo incluido."
            initialized = true
        }
    }

    /** Fetches from the public list; a failed or malformed download never replaces the last copy. */
    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        initialize(context)
        refreshLock.withLock refreshLock@ {
            publishPending(context)
            mutableSyncStatus.value = "Buscando novedades…"
            val text = try {
                RemoteCatalogClient().fetch()
            } catch (_: IOException) {
                mutableSyncStatus.value = "No se pudo actualizar. Se conserva la última lista disponible."
                return@refreshLock false
            }
            currentCoroutineContext().ensureActive()
            val saved = stateLock.withLock saveLock@ {
                val next = updateStoredCatalog(snapshot, text) ?: return@saveLock false
                try {
                    CatalogFileStore(context).write(next)
                } catch (_: IOException) {
                    return@saveLock false
                }
                val changed = snapshot?.content != next.content
                snapshot = next
                if (changed || playlists.value?.errorMessage != null) playlists.value = parseRemoteCatalog(next.content)
                true
            }
            if (!saved) {
                mutableSyncStatus.value = "No se pudo guardar la actualización. Se conserva la lista anterior."
                return@refreshLock false
            }
            mutableSyncStatus.value = "Catálogo actualizado desde la lista en línea."
            publishPending(context)
            true
        }
    }

    /** Called again after permission is granted; notification data survives denied permission. */
    suspend fun publishPending(context: Context) = withContext(Dispatchers.IO) {
        initialize(context)
        stateLock.withLock {
            val current = snapshot ?: return@withLock
            if (current.pending.isEmpty() || !CatalogNotifications.show(context, current.pending)) return@withLock
            val cleared = current.copy(pending = emptyList())
            try {
                CatalogFileStore(context).write(cleared)
                snapshot = cleared
            } catch (_: IOException) {
                // Retain pending in memory too; a deterministic notification ID prevents duplicates.
            }
        }
    }
}
