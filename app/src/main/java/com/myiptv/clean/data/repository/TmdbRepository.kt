package com.myiptv.clean.data.repository

import com.myiptv.clean.BuildConfig
import com.myiptv.clean.data.api.TmdbApi
import com.myiptv.clean.data.model.TmdbEpisodeResponse
import com.myiptv.clean.data.model.TmdbMediaResponse
import com.myiptv.clean.data.model.TmdbPageResponse
import com.myiptv.clean.ui.compose.MediaEpisode
import com.myiptv.clean.ui.compose.MediaItem
import com.myiptv.clean.ui.compose.MediaKind
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

object TmdbRepository {
    private const val API_BASE_URL = "https://api.themoviedb.org/3/"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    private const val LANGUAGE = "es-PE"

    val isConfigured: Boolean
        get() = BuildConfig.TMDB_API_KEY.isNotBlank()

    private val api: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    private data class CachedMedia(val item: MediaItem, val hasDetails: Boolean)

    private val mediaCache = ConcurrentHashMap<String, CachedMedia>()
    private var movieGenresById: Map<Int, String> = emptyMap()
    private var seriesGenresById: Map<Int, String> = emptyMap()

    suspend fun popularMovies(): List<MediaItem> {
        val response = api.popularMovies(BuildConfig.TMDB_API_KEY, LANGUAGE, 1)
        return response.toMediaItems("movie", movieGenresById)
    }

    suspend fun popularSeries(): List<MediaItem> {
        val response = api.popularSeries(BuildConfig.TMDB_API_KEY, LANGUAGE, 1)
        return response.toMediaItems("tv", seriesGenresById)
    }

    suspend fun trendingThisWeek(): List<MediaItem> =
        api.trendingThisWeek(BuildConfig.TMDB_API_KEY, LANGUAGE).toMixedMediaItems()

    suspend fun discoverMovies(genreId: Int): List<MediaItem> =
        api.discoverMovies(BuildConfig.TMDB_API_KEY, LANGUAGE, genreId).toMediaItems("movie", movieGenresById)

    suspend fun discoverSeries(genreId: Int, originCountry: String? = null): List<MediaItem> =
        api.discoverSeries(BuildConfig.TMDB_API_KEY, LANGUAGE, genreId, originCountry).toMediaItems("tv", seriesGenresById)

    suspend fun genreNames(kind: MediaKind): List<String> {
        val response = if (kind == MediaKind.MOVIE) {
            api.movieGenres(BuildConfig.TMDB_API_KEY, LANGUAGE).also { genres ->
                movieGenresById = genres.genres.orEmpty().mapNotNull { genre -> genre.name?.let { genre.id to it } }.toMap()
            }
        } else {
            api.seriesGenres(BuildConfig.TMDB_API_KEY, LANGUAGE).also { genres ->
                seriesGenresById = genres.genres.orEmpty().mapNotNull { genre -> genre.name?.let { genre.id to it } }.toMap()
            }
        }
        return listOf("Todos") + response.genres.orEmpty().mapNotNull { it.name }
    }

    suspend fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val response = api.searchMulti(BuildConfig.TMDB_API_KEY, LANGUAGE, query.trim())
        return response.toMixedMediaItems()
    }

    /** Resolve an M3U title without ever changing its local ID or its video source. */
    suspend fun playlistMetadata(local: MediaItem): MediaItem? {
        if (!isConfigured) return null
        return resolvePlaylistTmdbMetadata(
            local = local,
            search = { search ->
                val match = findPlaylistTmdbMatch(search) { request, page ->
                    if (request.kind == MediaKind.MOVIE) {
                        api.searchMovies(BuildConfig.TMDB_API_KEY, LANGUAGE, request.title, request.year, page = page)
                    } else {
                        api.searchSeries(BuildConfig.TMDB_API_KEY, LANGUAGE, request.title, request.year, page = page)
                    }
                }
                val type = if (search.kind == MediaKind.MOVIE) "movie" else "tv"
                match?.toMediaItem(type, genreMapFor(type))?.also(::cacheSummary)
            },
            detail = { id, type -> details("tmdb_${type}_$id") }
        )
    }

    suspend fun details(itemId: String): MediaItem? {
        mediaCache[itemId]?.takeIf(CachedMedia::hasDetails)?.let { return it.item }
        val parsed = parseTmdbId(itemId) ?: return mediaCache[itemId]?.item
        val (tmdbId, mediaType) = parsed
        val detail = if (mediaType == "movie") {
            api.movieDetails(tmdbId, BuildConfig.TMDB_API_KEY, LANGUAGE)
        } else {
            api.seriesDetails(tmdbId, BuildConfig.TMDB_API_KEY, LANGUAGE)
        }
        if (detail.id != tmdbId) return null
        return detail.toMediaItem(mediaType, genreMapFor(mediaType)).also { item ->
            synchronized(mediaCache) { mediaCache[item.id] = CachedMedia(item, hasDetails = true) }
        }
    }

    suspend fun seasonEpisodes(seriesId: Long, seasonNumber: Int): List<MediaEpisode> {
        val response = api.seasonDetails(seriesId, seasonNumber, BuildConfig.TMDB_API_KEY, LANGUAGE)
        return response.episodes.orEmpty().map { it.toMediaEpisode() }
    }

    fun cachedMedia(itemId: String): MediaItem? = mediaCache[itemId]?.item

    fun cachedCatalog(): List<MediaItem> = mediaCache.values.map(CachedMedia::item)

    private fun TmdbPageResponse.toMediaItems(
        mediaType: String,
        genreMap: Map<Int, String>
    ): List<MediaItem> = results.orEmpty().map { media ->
        media.toMediaItem(mediaType, genreMap).also(::cacheSummary)
    }

    /** Only movies and series from mixed search/trending responses can be opened as media. */
    internal fun TmdbPageResponse.toMixedMediaItems(): List<MediaItem> =
        results.orEmpty().mapNotNull { media ->
            val type = media.mediaType?.takeIf { it == "movie" || it == "tv" } ?: return@mapNotNull null
            media.toMediaItem(type, genreMapFor(type)).also(::cacheSummary)
        }

    private fun cacheSummary(item: MediaItem) {
        // Trending/discover requests can finish after a detail request; keep the richer ficha.
        synchronized(mediaCache) {
            if (mediaCache[item.id]?.hasDetails != true) {
                mediaCache[item.id] = CachedMedia(item, hasDetails = false)
            }
        }
    }

    private fun TmdbMediaResponse.toMediaItem(
        mediaType: String,
        genreMap: Map<Int, String>
    ): MediaItem {
        val kind = if (mediaType == "tv") MediaKind.SERIES else MediaKind.MOVIE
        val genreNames = genres.orEmpty().mapNotNull { it.name }
        val itemGenres = genreNames.ifEmpty { genreIds.orEmpty().mapNotNull(genreMap::get) }
        val year = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0
        val runtimeMinutes = runtime ?: episodeRunTime.orEmpty().firstOrNull()
        val duration = if (kind == MediaKind.SERIES) {
            numberOfSeasons?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "temporada" else "temporadas"}" } ?: "—"
        } else {
            runtimeMinutes?.takeIf { it > 0 }?.let(::formatRuntime) ?: "—"
        }
        val colors = colorPair(id)

        return MediaItem(
            id = "tmdb_${mediaType}_$id",
            title = title ?: name ?: "Sin título",
            kind = kind,
            year = year,
            rating = ((voteAverage ?: 0.0) * 10.0).toInt() / 10.0,
            genres = itemGenres,
            synopsis = overview.orEmpty().ifBlank { "Sinopsis no disponible." },
            duration = duration,
            colorA = colors.first,
            colorB = colors.second,
            posterUrl = posterPath?.let { IMAGE_BASE_URL + "w500" + it },
            backdropUrl = backdropPath?.let { IMAGE_BASE_URL + "w1280" + it },
            tmdbId = id,
            tmdbMediaType = mediaType,
            castNames = credits?.cast.orEmpty().take(12).mapNotNull { cast -> cast.name?.takeIf(String::isNotBlank) },
            seasonCount = numberOfSeasons,
            episodeCount = numberOfEpisodes
        )
    }

    private fun TmdbEpisodeResponse.toMediaEpisode() = MediaEpisode(
        number = episodeNumber,
        title = name?.takeIf(String::isNotBlank) ?: "Episodio $episodeNumber",
        duration = runtime?.let(::formatRuntime) ?: "—",
        synopsis = overview?.takeIf(String::isNotBlank) ?: "Sinopsis no disponible.",
        stillUrl = stillPath?.let { IMAGE_BASE_URL + "w500" + it }
    )

    private fun genreMapFor(mediaType: String): Map<Int, String> =
        if (mediaType == "movie") movieGenresById else seriesGenresById

    private fun parseTmdbId(itemId: String): Pair<Long, String>? {
        val parts = itemId.split('_')
        if (parts.size != 3 || parts[0] != "tmdb" || parts[1] !in setOf("movie", "tv")) return null
        val id = parts[2].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return id to parts[1]
    }

    private fun formatRuntime(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours == 0) "$minutes min" else "$hours h $remainingMinutes min"
    }

    private fun colorPair(seed: Long): Pair<Long, Long> {
        val palettes = listOf(
            0xFF6642A6L to 0xFF1A284CL,
            0xFFB95139L to 0xFF321D45L,
            0xFF087CA7L to 0xFF082C58L,
            0xFF42865EL to 0xFF172C42L,
            0xFFAC426BL to 0xFF3B1B47L,
            0xFF8A633CL to 0xFF2D203BL
        )
        return palettes[(seed % palettes.size).toInt()]
    }
}
