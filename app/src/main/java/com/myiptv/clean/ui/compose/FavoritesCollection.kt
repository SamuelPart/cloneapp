package com.myiptv.clean.ui.compose

import com.myiptv.clean.data.repository.MediaPlaylist

internal interface FavoritesStore {
    fun read(): Set<String>
    fun write(ids: Set<String>)
}

/** The UI keeps local favorites across process restarts and same-signature app updates. */
internal class FavoritesCollection(private val store: FavoritesStore) {
    var savedIds: Set<String> = store.read().toSet()
        private set

    fun toggle(id: String, playlist: MediaPlaylist): Set<String> {
        val aliases = playlist.mediaAliases(id)
        val next = if (savedIds.any { it in aliases }) savedIds - aliases
        else savedIds + playlist.canonicalMediaId(id)
        return replace(next)
    }

    fun canonicalize(playlist: MediaPlaylist): Set<String> =
        replace(savedIds.mapTo(mutableSetOf(), playlist::canonicalMediaId))

    private fun replace(ids: Set<String>): Set<String> {
        val next = ids.toSet()
        if (next != savedIds) {
            store.write(next)
            savedIds = next
        }
        return savedIds
    }
}
