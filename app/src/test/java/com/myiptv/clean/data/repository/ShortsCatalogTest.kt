package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaItem
import com.myiptv.clean.ui.compose.MediaKind
import com.myiptv.clean.ui.compose.MediaPlaybackType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsCatalogTest {
    @Test
    fun generatedClipsUseNinetyToOneHundredFiftySecondsAndSafeRandomOffsets() {
        val clips = recommendShortClips(
            candidates = listOf(candidate("movie-a", duration = "120 min"), candidate("movie-b", duration = "90 min")),
            genreCounts = emptyMap(),
            random = Random(23)
        )

        assertEquals(2, clips.size)
        clips.forEach { clip ->
            assertTrue(clip.durationMs in 90_000L..150_000L)
            val maxStart = (parseRuntimeMs(clip.candidate.media.duration)!! - clip.durationMs).coerceAtLeast(0L)
            assertTrue(clip.startAtMs != null && clip.startAtMs in 0L..maxStart)
        }
    }

    @Test
    fun shortsOnlyUseDirectCatalogSourcesAndKeepEpisodeParentForDetails() {
        val series = candidate("m3u_tv_abcdef", "45 min").media.copy(kind = MediaKind.SERIES)
        val playlist = MediaPlaylist(
            items = listOf(series),
            sources = mapOf(
                "m3u_tv_abcdef_s1_e1" to PlaylistSource("https://example.test/episode.mp4", MediaPlaybackType.DIRECT),
                "m3u_tv_abcdef_s1_e2" to PlaylistSource("https://example.test/embed", MediaPlaybackType.IFRAME)
            )
        )

        val shorts = shortsCandidates(playlist)

        assertEquals(1, shorts.size)
        assertEquals("m3u_tv_abcdef_s1_e1", shorts.single().playbackId)
        assertEquals(series.id, shorts.single().media.id)
    }

    @Test
    fun genreAffinityIncreasesRecommendationWeightWithoutRemovingExploration() {
        val favored = recommendationWeight(listOf("Drama"), mapOf("drama" to 8))
        val newGenre = recommendationWeight(listOf("Documentary"), mapOf("drama" to 8))

        assertTrue(favored > newGenre)
        assertTrue(newGenre > 0.0)
    }

    @Test
    fun preferredGenresAppearMoreOftenInARepeatedFeedBatch() {
        val drama = candidate("drama", "90 min", "Drama")
        val comedy = candidate("comedy", "90 min", "Comedy")
        val clips = recommendShortClips(
            candidates = listOf(drama, comedy),
            genreCounts = mapOf("drama" to 100),
            random = Random(7),
            batchSize = 30
        )

        assertTrue(clips.count { it.candidate.media.genres.contains("Drama") } > clips.count { it.candidate.media.genres.contains("Comedy") })
    }

    @Test
    fun contentTypeContributesToTheLocalTasteProfile() {
        val movie = candidate("movie", "90 min").media
        val labels = shortsTasteLabels(movie)

        assertTrue(labels.any { it.equals("Película", ignoreCase = true) })
        assertTrue(recommendationWeight(labels, mapOf("película" to 5)) > recommendationWeight(labels, emptyMap()))
    }

    @Test
    fun runtimeParserHandlesHoursMinutesAndUnknownValues() {
        assertEquals(5_400_000L, parseRuntimeMs("1 h 30 min"))
        assertEquals(7_200_000L, parseRuntimeMs("120 min"))
        assertEquals(null, parseRuntimeMs("—"))
    }

    private fun candidate(id: String, duration: String, genre: String = "Drama"): ShortsCandidate {
        val media = MediaItem(
            id = id,
            title = id,
            kind = MediaKind.MOVIE,
            year = 2025,
            rating = 0.0,
            genres = listOf(genre),
            synopsis = "",
            duration = duration,
            colorA = 0L,
            colorB = 0L
        )
        return ShortsCandidate(media, id, PlaylistSource("https://example.test/$id.mp4", MediaPlaybackType.DIRECT))
    }
}
