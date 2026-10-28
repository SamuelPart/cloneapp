package com.myiptv.clean.data.repository

import com.myiptv.clean.data.api.TmdbApi
import com.myiptv.clean.data.model.TmdbMediaResponse
import com.myiptv.clean.data.model.TmdbPageResponse
import com.myiptv.clean.ui.compose.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET

class TmdbMixedResultsTest {
    @Test
    fun weeklyTrendsUseTheWeeklyMixedEndpoint() {
        val method = TmdbApi::class.java.declaredMethods.first { it.name == "trendingThisWeek" }
        assertEquals("trending/all/week", method.getAnnotation(GET::class.java)?.value)
    }

    @Test
    fun mixedResultsKeepOnlyMoviesAndSeriesInTheirSourceOrder() {
        val response = TmdbPageResponse(
            listOf(
                TmdbMediaResponse(id = 42, mediaType = "movie", title = "Película", releaseDate = "2020-02-01", posterPath = "/poster.jpg"),
                TmdbMediaResponse(id = 13, mediaType = "person", name = "Persona"),
                TmdbMediaResponse(id = 42, mediaType = "tv", name = "Serie", firstAirDate = "2021-03-01", backdropPath = "/backdrop.jpg"),
                TmdbMediaResponse(id = 14, mediaType = null, name = "Sin tipo")
            )
        )

        val results = with(TmdbRepository) { response.toMixedMediaItems() }
        assertEquals(listOf("tmdb_movie_42", "tmdb_tv_42"), results.map { it.id })
        assertEquals(listOf(MediaKind.MOVIE, MediaKind.SERIES), results.map { it.kind })
        assertEquals(listOf(2020, 2021), results.map { it.year })
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", results[0].posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w1280/backdrop.jpg", results[1].backdropUrl)
        assertEquals(results[0], TmdbRepository.cachedMedia(results[0].id))
    }

    @Test
    fun missingResultsProduceAnEmptyList() {
        assertTrue(with(TmdbRepository) { TmdbPageResponse().toMixedMediaItems() }.isEmpty())
    }
}
