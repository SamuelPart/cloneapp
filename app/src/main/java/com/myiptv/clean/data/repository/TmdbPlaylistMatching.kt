package com.myiptv.clean.data.repository

import com.myiptv.clean.data.model.TmdbMediaResponse
import com.myiptv.clean.data.model.TmdbPageResponse
import com.myiptv.clean.ui.compose.MediaItem
import com.myiptv.clean.ui.compose.MediaKind
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.CancellationException

internal data class PlaylistTmdbSearch(val kind: MediaKind, val title: String, val year: Int?)

private const val MAX_MATCH_PAGES = 5
private val trailingYear = Regex("""\s+\(((?:19|20)\d{2})\)\s*$""")
private val combiningMarks = Regex("\\p{M}+")
private val titleSeparators = Regex("[^\\p{L}\\p{N}]+")

/** The year in a title or M3U attribute must agree; a conflict is not a safe match. */
internal fun playlistTmdbSearch(item: MediaItem): PlaylistTmdbSearch? {
    val yearMatch = trailingYear.find(item.title)
    val titleYear = yearMatch?.groupValues?.get(1)?.toIntOrNull()
    val attributeYear = item.year.takeIf { it > 0 }
    if (titleYear != null && attributeYear != null && titleYear != attributeYear) return null
    val title = (if (yearMatch != null) item.title.substring(0, yearMatch.range.first) else item.title).trim()
    if (normalizedTmdbTitle(title).isBlank()) return null
    return PlaylistTmdbSearch(item.kind, title, attributeYear ?: titleYear)
}

/** Explicit IDs take precedence; summaries are a fallback only if the full detail request fails. */
internal suspend fun resolvePlaylistTmdbMetadata(
    local: MediaItem,
    search: suspend (PlaylistTmdbSearch) -> MediaItem?,
    detail: suspend (Long, String) -> MediaItem?
): MediaItem? {
    val type = if (local.kind == MediaKind.MOVIE) "movie" else "tv"
    if (local.tmdbMediaType != null && local.tmdbMediaType != type) return null
    val pinned = local.tmdbId?.takeIf { it > 0 }
    val summary = if (pinned == null) search(playlistTmdbSearch(local) ?: return null) ?: return null else null
    val id = pinned ?: summary?.tmdbId?.takeIf { it > 0 } ?: return null
    val remote = try {
        detail(id, type)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        summary ?: throw error
    } ?: summary ?: return null
    if (remote.kind != local.kind || remote.tmdbMediaType != type || remote.tmdbId != id) return null
    return mergePlaylistTmdbMetadata(local, remote)
}

private fun normalizedTmdbTitle(title: String): String {
    val decomposed = Normalizer.normalize(title, Normalizer.Form.NFD).lowercase(Locale.ROOT)
    return titleSeparators.replace(combiningMarks.replace(decomposed, ""), " ").trim()
}

/**
 * Query the correct media type, then require exactly one exact normalized title/original-title
 * and, when supplied, the same release year. Do not choose from an incomplete result set.
 */
internal suspend fun findPlaylistTmdbMatch(
    search: PlaylistTmdbSearch,
    fetchPage: suspend (PlaylistTmdbSearch, Int) -> TmdbPageResponse
): TmdbMediaResponse? {
    val first = fetchPage(search, 1)
    val totalPages = (first.totalPages ?: 1).coerceAtLeast(1)
    if (totalPages > MAX_MATCH_PAGES) return null
    val candidates = first.results.orEmpty().toMutableList()
    for (page in 2..totalPages) candidates += fetchPage(search, page).results.orEmpty()

    val expectedTitle = normalizedTmdbTitle(search.title)
    val expectedType = if (search.kind == MediaKind.MOVIE) "movie" else "tv"
    return candidates.asSequence()
        .filter { it.id > 0 && (it.mediaType == null || it.mediaType == expectedType) }
        .filter { candidate ->
            val names = if (search.kind == MediaKind.MOVIE) {
                listOf(candidate.title, candidate.originalTitle)
            } else {
                listOf(candidate.name, candidate.originalName)
            }
            names.filterNotNull().any { normalizedTmdbTitle(it) == expectedTitle }
        }
        .filter { candidate ->
            val date = if (search.kind == MediaKind.MOVIE) candidate.releaseDate else candidate.firstAirDate
            search.year == null || date?.take(4)?.toIntOrNull() == search.year
        }
        .distinctBy(TmdbMediaResponse::id)
        .toList()
        .singleOrNull()
}
