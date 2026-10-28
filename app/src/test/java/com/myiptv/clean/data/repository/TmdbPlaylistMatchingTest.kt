package com.myiptv.clean.data.repository

import com.myiptv.clean.data.api.TmdbApi
import com.myiptv.clean.data.model.TmdbMediaResponse
import com.myiptv.clean.data.model.TmdbPageResponse
import com.myiptv.clean.ui.compose.MediaKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class TmdbPlaylistMatchingTest {
    private fun catalog(): MediaPlaylist = MediaPlaylistParser.parse(
        """
        #EXTM3U
        #EXTINF:-1 group-title="Peliculas",Marsupilami (2026)
        https://example.com/watch/movie
        #EXTINF:-1 group-title="La Brea Temporada 1",Capítulo 1
        https://example.com/watch/t1e1
        #EXTINF:-1 group-title="La Brea Temporada 2",Capítulo 2
        https://example.com/watch/t2e2
        """.trimIndent()
    )

    @Test
    fun packagedTitlePinsDoNotChangeAnyOriginalSourcesOrEpisodeKeys() {
        val asset = listOf(File("src/main/assets/samplay_catalog.m3u"), File("app/src/main/assets/samplay_catalog.m3u"))
            .firstOrNull(File::isFile) ?: error("No se encontró el catálogo incluido")
        val text = asset.readText(Charsets.UTF_8)
        val pinned = MediaPlaylistParser.parse(text)
        val unpinned = MediaPlaylistParser.parse(text.replace(Regex("""\s+tmdb-match-id="\d+""""), ""))
        val movie = pinned.items.first { it.title == "Marsupilami (2026)" }
        val series = pinned.items.first { it.title == "La Brea" }
        assertEquals(1145899L, movie.tmdbId)
        assertEquals(128839L, series.tmdbId)
        assertEquals(unpinned.items.map { it.id }, pinned.items.map { it.id })
        assertEquals(unpinned.sources, pinned.sources)
        assertEquals(unpinned.episodes, pinned.episodes)
        assertEquals(listOf(1, 2), pinned.seasons(series.id))
        assertEquals(10, pinned.episodes(series.id, 1).size)
        assertEquals(2, pinned.episodes(series.id, 2).size)
        assertEquals(0, pinned.ignoredEntries)
    }

    @Test
    fun metadataOnlyIdKeepsExistingM3uIdsAndRejectsConflictingEpisodeIds() {
        val plain = catalog()
        val withIds = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="Peliculas" tmdb-match-id="1145899",Marsupilami (2026)
            https://example.com/watch/movie
            #EXTINF:-1 group-title="La Brea Temporada 1" tmdb-match-id="128839",Capítulo 1
            https://example.com/watch/t1e1
            #EXTINF:-1 group-title="La Brea Temporada 2",Capítulo 2
            https://example.com/watch/t2e2
            #EXTINF:-1 group-title="La Brea Temporada 2" tmdb-match-id="999",Capítulo 3
            https://example.com/watch/conflicting
            """.trimIndent()
        )
        assertEquals(plain.items.map { it.id }, withIds.items.map { it.id })
        assertEquals(plain.sources, withIds.sources)
        assertEquals(1, withIds.ignoredEntries)
        assertEquals(1145899L, withIds.items.first { it.kind == MediaKind.MOVIE }.tmdbId)
        assertEquals(128839L, withIds.items.first { it.kind == MediaKind.SERIES }.tmdbId)
        assertEquals("https://example.com/watch/movie", withIds.source("tmdb_movie_1145899")?.url)
        assertEquals("https://example.com/watch/t1e1", withIds.source("tmdb_tv_128839_s1_e1")?.url)
    }

    @Test
    fun metadataOnlyIdRejectsInvalidOrContradictoryExplicitIds() {
        val playlist = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="Peliculas" tmdb-match-id="0",Invalida
            https://example.com/a.mp4
            #EXTINF:-1 media-type="movie" tmdb-id="4" tmdb-match-id="5",Contradictoria
            https://example.com/b.mp4
            #EXTINF:-1 group-title="Peliculas" tmdb-match-id="abc",Invalida
            https://example.com/c.mp4
            """.trimIndent()
        )
        assertTrue(playlist.items.isEmpty())
        assertTrue(playlist.sources.isEmpty())
        assertEquals(3, playlist.ignoredEntries)
    }

    @Test
    fun requestsUseTheCorrectTypeSpecificSearchEndpoints() {
        val methods = TmdbApi::class.java.declaredMethods.associateBy { it.name }
        assertEquals("search/movie", methods["searchMovies"]?.getAnnotation(GET::class.java)?.value)
        assertEquals("search/tv", methods["searchSeries"]?.getAnnotation(GET::class.java)?.value)
    }

    @Test
    fun matchesOriginalTitleAndSuppliedYearWithoutChangingTheMovieIdOrSource() = runBlocking {
        val playlist = catalog()
        val local = playlist.items.first { it.kind == MediaKind.MOVIE }
        val search = playlistTmdbSearch(local)!!
        assertEquals("Marsupilami", search.title)
        assertEquals(2026, search.year)
        val match = findPlaylistTmdbMatch(search) { request, page ->
            assertEquals(MediaKind.MOVIE, request.kind)
            assertEquals(1, page)
            TmdbPageResponse(listOf(
                TmdbMediaResponse(id = 3, title = "Marsupilami", releaseDate = "2012-04-04"),
                TmdbMediaResponse(id = 1145899, title = "Título localizado", originalTitle = "Marsupilami", releaseDate = "2026-02-04", posterPath = "/poster.jpg")
            ), totalPages = 1)
        }
        assertEquals(1145899L, match?.id)
        val remote = local.copy(
            id = "tmdb_movie_1145899", title = "Marsupilami", synopsis = "Sinopsis de TMDb",
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg", tmdbId = match!!.id, tmdbMediaType = "movie"
        )
        val enriched = mergePlaylistTmdbMetadata(local, remote)
        assertEquals(local.id, enriched.id)
        assertEquals("Sinopsis de TMDb", enriched.synopsis)
        assertEquals("https://example.com/watch/movie", playlist.source(enriched.id)?.url)
        assertEquals(listOf(enriched.id), mergeMediaCatalog(listOf(enriched), listOf(remote)).map { it.id })
    }

    @Test
    fun matchesTheSeriesOriginalNameAndKeepsEverySeasonAndEpisodeLink() = runBlocking {
        val playlist = catalog()
        val local = playlist.items.first { it.kind == MediaKind.SERIES }
        val search = playlistTmdbSearch(local)!!
        val match = findPlaylistTmdbMatch(search) { request, _ ->
            assertEquals(MediaKind.SERIES, request.kind)
            TmdbPageResponse(listOf(TmdbMediaResponse(id = 128839, name = "La Brea: La tierra perdida", originalName = "La Brea", firstAirDate = "2021-09-28")))
        }
        assertEquals(128839L, match?.id)
        val remote = local.copy(id = "tmdb_tv_128839", tmdbId = match!!.id, tmdbMediaType = "tv", seasonCount = 3, episodeCount = 30)
        val enriched = mergePlaylistTmdbMetadata(local, remote)
        assertEquals(local.id, enriched.id)
        assertEquals(3, enriched.seasonCount)
        assertEquals(listOf(1, 2), playlist.seasons(enriched.id))
        assertEquals("https://example.com/watch/t1e1", playlist.source(episodeSourceId(enriched.id, 1, 1))?.url)
        assertEquals("https://example.com/watch/t2e2", playlist.source(episodeSourceId(enriched.id, 2, 2))?.url)

        val linkedPlaylist = playlist.copy(items = playlist.items.map { if (it.id == local.id) enriched else it })
        assertEquals(local.id, linkedPlaylist.item("tmdb_tv_128839")?.id)
        assertEquals("https://example.com/watch/t2e2", linkedPlaylist.source("tmdb_tv_128839_s2_e2")?.url)
        assertEquals(listOf(1, 2), linkedPlaylist.seasons("tmdb_tv_128839"))
        assertEquals(setOf(local.id, "tmdb_tv_128839"), linkedPlaylist.expandedMediaIds(setOf("tmdb_tv_128839")))
        assertEquals(local.id, linkedPlaylist.canonicalMediaId("tmdb_tv_128839"))
        assertEquals(setOf(episodeSourceId(local.id, 2, 2), "tmdb_tv_128839_s2_e2"),
            linkedPlaylist.sourceAliases(episodeSourceId(local.id, 2, 2)))
    }

    @Test
    fun refusesAmbiguousWrongYearWrongTypePartialTitleOrIncompleteResults() = runBlocking {
        val movie = catalog().items.first { it.kind == MediaKind.MOVIE }
        val search = playlistTmdbSearch(movie)!!
        suspend fun choose(vararg candidates: TmdbMediaResponse): TmdbMediaResponse? =
            findPlaylistTmdbMatch(search) { _, _ -> TmdbPageResponse(candidates.toList()) }
        val correct = TmdbMediaResponse(id = 1, title = "Marsupilami", releaseDate = "2026-02-04")
        assertNull(choose(correct, correct.copy(id = 2)))
        assertNull(choose(correct.copy(releaseDate = "2025-01-01")))
        assertNull(choose(correct.copy(releaseDate = null)))
        assertNull(choose(correct.copy(mediaType = "tv")))
        assertNull(choose(correct.copy(title = "El regreso de Marsupilami")))
        assertNull(findPlaylistTmdbMatch(search) { _, _ -> TmdbPageResponse(listOf(correct), totalPages = 6) })
        assertNull(playlistTmdbSearch(movie.copy(year = 2025)))
    }

    @Test
    fun checksOtherPagesBeforeChoosingAndIgnoresRepeatedIds() = runBlocking {
        val search = playlistTmdbSearch(catalog().items.first { it.kind == MediaKind.SERIES })!!
        val seen = mutableListOf<Int>()
        val match = TmdbMediaResponse(id = 128839, originalName = "La Brea", firstAirDate = "2021-09-28")
        val duplicate = findPlaylistTmdbMatch(search) { _, page ->
            seen += page
            TmdbPageResponse(listOf(match), totalPages = 2)
        }
        assertEquals(listOf(1, 2), seen)
        assertEquals(match.id, duplicate?.id)
        assertNull(findPlaylistTmdbMatch(search) { _, page ->
            TmdbPageResponse(listOf(if (page == 1) match else match.copy(id = 2)), totalPages = 2)
        })
    }

    @Test
    fun matchingIsAccentAndPunctuationInsensitiveButNotFuzzy() = runBlocking {
        val local = catalog().items.first().copy(title = "El Niño: película (2026)")
        val search = playlistTmdbSearch(local)!!
        val candidate = TmdbMediaResponse(id = 7, title = "El Nino - pelicula", releaseDate = "2026-01-01")
        assertEquals(7L, findPlaylistTmdbMatch(search) { _, _ -> TmdbPageResponse(listOf(candidate)) }?.id)
        assertNull(findPlaylistTmdbMatch(search) { _, _ -> TmdbPageResponse(listOf(candidate.copy(title = "El Niño 2"))) })
    }

    @Test
    fun explicitMetadataIdLoadsDetailsDirectlyAndPreservesTheLocalIdentity() = runBlocking {
        val local = catalog().items.first { it.kind == MediaKind.MOVIE }
            .copy(tmdbId = 1145899, tmdbMediaType = "movie")
        val enriched = resolvePlaylistTmdbMetadata(local,
            search = { error("A pinned ID must not run a title search") },
            detail = { id, type ->
                assertEquals(1145899L, id)
                assertEquals("movie", type)
                local.copy(id = "tmdb_movie_$id", title = "Datos TMDb", castNames = listOf("Reparto"))
            }
        )
        assertEquals(local.id, enriched?.id)
        assertEquals("Datos TMDb", enriched?.title)
        assertEquals(listOf("Reparto"), enriched?.castNames)
    }

    @Test
    fun automaticMatchFallsBackToSearchMetadataOnDetailFailureButRejectsWrongIdentity() = runBlocking {
        val local = catalog().items.first { it.kind == MediaKind.SERIES }
        val summary = local.copy(id = "tmdb_tv_128839", tmdbId = 128839, tmdbMediaType = "tv", synopsis = "Resumen TMDb")
        val fallback = resolvePlaylistTmdbMetadata(local, search = { summary }, detail = { _, _ -> error("offline") })
        assertEquals(local.id, fallback?.id)
        assertEquals("Resumen TMDb", fallback?.synopsis)
        assertNull(resolvePlaylistTmdbMetadata(local, search = { summary }, detail = { _, _ -> summary.copy(tmdbId = 7) }))
    }

    @Test
    fun metadataLoaderLimitsConcurrentRequestsEvenForLargeCatalogs() = runBlocking {
        val seed = catalog().items.first()
        val playlist = MediaPlaylist(items = (1..12).map { seed.copy(id = "m3u_movie_$it") })
        val running = AtomicInteger(0)
        val highWater = AtomicInteger(0)
        val updates = mutableListOf<MediaPlaylist>()
        loadPlaylistMetadata(playlist, resolve = { item ->
            val active = running.incrementAndGet()
            highWater.updateAndGet { maxOf(it, active) }
            try {
                delay(10)
                item.copy(synopsis = "Ficha ${item.id}")
            } finally {
                running.decrementAndGet()
            }
        }, onUpdate = { updates += it })
        assertEquals(3, highWater.get())
        assertEquals(12, updates.size)
        assertEquals(12, updates.last().items.count { it.synopsis.startsWith("Ficha ") })
    }

    @Test
    fun progressivelyLoadsOtherItemsWhenOneFailsWithoutTouchingSources() = runBlocking {
        val playlist = catalog()
        val updates = mutableListOf<MediaPlaylist>()
        val inFlight = AtomicInteger(0)
        val highWater = AtomicInteger(0)
        loadPlaylistMetadata(playlist, resolve = { item ->
            val count = inFlight.incrementAndGet()
            highWater.updateAndGet { maxOf(it, count) }
            try {
                delay(10)
                if (item.kind == MediaKind.MOVIE) error("network failure")
                item.copy(synopsis = "Datos reales simulados", tmdbId = 128839, tmdbMediaType = "tv")
            } finally {
                inFlight.decrementAndGet()
            }
        }, onUpdate = { updates += it })
        assertEquals(1, updates.size)
        assertTrue(highWater.get() <= 3)
        assertEquals(playlist.sources, updates.last().sources)
        assertEquals(playlist.episodes, updates.last().episodes)
        assertEquals(playlist.items.map { it.id }, updates.last().items.map { it.id })
        assertEquals("Datos reales simulados", updates.last().items.first { it.kind == MediaKind.SERIES }.synopsis)
        assertFalse(updates.last().items.first { it.kind == MediaKind.MOVIE }.synopsis == "Datos reales simulados")
    }
}
