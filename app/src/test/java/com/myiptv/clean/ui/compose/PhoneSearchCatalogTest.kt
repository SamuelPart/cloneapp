package com.myiptv.clean.ui.compose

import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.PlaylistSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneSearchCatalogTest {
    private fun movie(id: String, title: String, tmdbId: Long? = null, genre: String = "Acción") = MediaItem(
        id = id, title = title, kind = MediaKind.MOVIE, year = 1994, rating = 7.0,
        genres = listOf(genre), synopsis = "", duration = "", colorA = 0, colorB = 0,
        tmdbId = tmdbId, tmdbMediaType = tmdbId?.let { "movie" }
    )

    private fun channel(id: String, name: String, category: String) = LiveChannel(
        id = id, name = name, category = category, logoLetters = "TV", streamUrl = "https://example.org/$id.m3u8", accent = 0
    )

    @Test
    fun searchesLocalTitlesGenresAndChannelsWithoutAccentsOrExtraSpaces() {
        val kid = movie("kid", "El Niño Feliz", genre = "Aventura")
        val other = movie("other", "Otra película")
        val news = channel("news", "Señal Perú", "Noticias")
        val mediaIndex = PhoneMediaSearchIndex(listOf(kid, other))
        val channelIndex = PhoneChannelSearchIndex(listOf(news, news))

        assertEquals("el nino feliz", normalizePhoneSearch("  EL  Niño FELIZ "))
        assertEquals(listOf(kid), mediaIndex.matches("  nino  feliz  "))
        assertEquals(listOf(kid), mediaIndex.matches("aventura"))
        assertEquals(listOf(other), mediaIndex.matches("ACCION"))
        assertEquals(listOf(news), channelIndex.matches("  senal PERU "))
        assertEquals(listOf(news), channelIndex.matches("noticias"))
        assertTrue(mediaIndex.matches("   ").isEmpty())
        assertTrue(channelIndex.matches("").isEmpty())
    }

    @Test
    fun matchingDuplicateTitlesKeepAllTheirOriginalPlaylistIds() {
        val first = movie("m3u_1", "¿Quién soy?", tmdbId = 123)
        val second = movie("m3u_2", "¿Quién soy?", tmdbId = 123)
        val third = movie("m3u_3", "¿Quién soy?", tmdbId = 123)
        val tmdb = movie("tmdb_movie_123", "¿Quién soy?", tmdbId = 123)
        val index = PhoneMediaSearchIndex(listOf(first, second, third))

        assertEquals(listOf("m3u_1", "m3u_2", "m3u_3"), phoneSearchMedia(index, "quien soy", listOf(tmdb)).map { it.id })
        assertTrue(phoneSearchMedia(index, " ", listOf(tmdb)).isEmpty())
    }

    @Test
    fun typedSearchMergesLocalAndTmdbWithoutLosingTheLocalIdentity() {
        val local = movie("m3u_local", "El niño", tmdbId = 42)
        val alias = movie("tmdb_movie_42", "El niño", tmdbId = 42).copy(posterUrl = "https://image.example/42.jpg")
        val remoteOnly = movie("tmdb_movie_99", "Niño nuevo", tmdbId = 99)
        val results = phoneSearchMedia(PhoneMediaSearchIndex(listOf(local)), "nino", listOf(alias, remoteOnly))

        assertEquals(listOf("m3u_local", "tmdb_movie_99"), results.map { it.id })
        assertEquals(alias.posterUrl, results.first().posterUrl)
        assertEquals(listOf(local), phoneSearchMedia(PhoneMediaSearchIndex(listOf(local)), "n", emptyList()))
    }

    @Test
    fun suggestionsUseRealPopularTitlesAndVerifiedM3uSources() {
        val local = movie("m3u_original", "El tonto", tmdbId = 8467)
        val localOnly = movie("m3u_other", "Título local")
        val remote = movie("tmdb_movie_8467", "Dos tontos", tmdbId = 8467).copy(posterUrl = "https://image.example/poster.jpg")
        val remoteOnly = movie("tmdb_movie_999", "Título remoto", tmdbId = 999)
        val source = PlaylistSource("https://example.org/video.mp4", MediaPlaybackType.DIRECT)
        val playlist = MediaPlaylist(items = listOf(local, localOnly), sources = mapOf(local.id to source))

        val suggestions = phoneSearchSuggestions(playlist, listOf(remote, remoteOnly, remote))
        assertEquals(listOf("m3u_original", "tmdb_movie_999"), suggestions.map { it.id })
        assertEquals(remote.posterUrl, suggestions.first().posterUrl)
        assertEquals(source, playlist.source(suggestions.first().id))
        assertEquals(listOf(local, localOnly), phoneSearchSuggestions(playlist, emptyList()))
    }

    @Test
    fun popularMovieAndSeriesListsAreInterleavedWithoutDuplicates() {
        val first = movie("movie1", "Película 1")
        val second = movie("movie2", "Película 2")
        val series = movie("series1", "Serie").copy(kind = MediaKind.SERIES)
        assertEquals(listOf(first, series, second), phonePopularTitles(listOf(first, second, first), listOf(series)))
    }

    @Test
    fun refreshCyclesTenRealSuggestionsAtATimeAndWrapsSafely() {
        val titles = (1..25).map { movie("id_$it", "Título $it") }
        assertEquals((1..10).toList(), phoneSuggestionPage(titles, 0).map { it.position })
        assertEquals((11..20).toList(), phoneSuggestionPage(titles, 1).map { it.position })
        assertEquals((21..25).toList(), phoneSuggestionPage(titles, 2).map { it.position })
        assertEquals(phoneSuggestionPage(titles, 0), phoneSuggestionPage(titles, 3))
        assertEquals(phoneSuggestionPage(titles, 2), phoneSuggestionPage(titles, -1))
        assertEquals(10, phoneSuggestionPage(titles + titles.first(), 0).size)
        assertTrue(phoneSuggestionPage(emptyList(), 0).isEmpty())
    }
}
