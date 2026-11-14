package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCatalogOrderingTest {
    @Test
    fun popularCatalogKeepsTmdbOrderAndPlacesUnmatchedPlaylistItemsAfterIt() {
        val tmdbFirst = media("tmdb_movie_20", tmdbId = 20L)
        val tmdbSecond = media("tmdb_movie_10", tmdbId = 10L)
        val matchedFirst = media("m3u_movie_a", tmdbId = 10L)
        val matchedSecond = media("m3u_movie_b", tmdbId = 20L)
        val localOnly = media("m3u_movie_local")

        val ordered = orderPopularCatalogItems(
            items = listOf(matchedFirst, localOnly, matchedSecond),
            tmdbOrder = listOf(tmdbFirst, tmdbSecond),
        )

        assertEquals(listOf(matchedSecond, matchedFirst, localOnly), ordered)
    }

    private fun media(id: String, tmdbId: Long? = null) = MediaItem(
        id = id,
        title = id,
        kind = MediaKind.MOVIE,
        year = 2024,
        rating = 7.0,
        genres = emptyList(),
        synopsis = "",
        duration = "—",
        colorA = 0L,
        colorB = 0L,
        tmdbId = tmdbId,
        tmdbMediaType = tmdbId?.let { "movie" },
    )
}
