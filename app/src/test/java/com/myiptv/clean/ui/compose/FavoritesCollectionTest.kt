package com.myiptv.clean.ui.compose

import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.MediaPlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesCollectionTest {
    private class MemoryStore(initial: Set<String> = emptySet()) : FavoritesStore {
        var stored = initial.toSet()
        var writes = 0
        override fun read(): Set<String> = stored.toSet()
        override fun write(ids: Set<String>) { stored = ids.toSet(); writes++ }
    }

    private fun playlist(): MediaPlaylist = MediaPlaylistParser.parse(
        """
        #EXTM3U
        #EXTINF:-1 group-title="Peliculas" tmdb-match-id="42",Obra de prueba
        https://video.example/movie
        """.trimIndent()
    )

    @Test fun favoritesAreStoredAndRestoredAcrossNewSessions() {
        val store = MemoryStore()
        val list = playlist()
        val id = list.items.single().id
        val first = FavoritesCollection(store)
        first.toggle(id, list)
        first.toggle("channel-id", list)
        assertEquals(setOf(id, "channel-id"), FavoritesCollection(store).savedIds)
        assertEquals(2, store.writes)
        FavoritesCollection(store).toggle(id, list)
        assertEquals(setOf("channel-id"), FavoritesCollection(store).savedIds)
    }

    @Test fun aliasesAreCanonicalizedWithoutDroppingUnavailableFavorites() {
        val list = playlist()
        val canonicalId = list.items.single().id
        val store = MemoryStore(setOf("tmdb_movie_42", "offline-channel"))
        val favorites = FavoritesCollection(store)
        favorites.canonicalize(list)
        assertEquals(setOf(canonicalId, "offline-channel"), favorites.savedIds)
        assertEquals(favorites.savedIds, FavoritesCollection(store).savedIds)
        favorites.canonicalize(MediaPlaylist())
        assertEquals(setOf(canonicalId, "offline-channel"), favorites.savedIds)
        favorites.toggle("tmdb_movie_42", list)
        assertEquals(setOf("offline-channel"), favorites.savedIds)
        assertTrue("No se borra ningún favorito ausente temporalmente", "offline-channel" in store.stored)
    }
}
