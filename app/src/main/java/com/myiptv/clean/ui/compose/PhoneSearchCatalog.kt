package com.myiptv.clean.ui.compose

import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.mergeMediaCatalog
import java.text.Normalizer
import java.util.Locale

private val searchMarks = Regex("\\p{M}+")
private val searchWhitespace = Regex("\\s+")
internal const val PHONE_SEARCH_SUGGESTIONS_PER_PAGE = 10

/** Accent-insensitive lookup is local to phone search; no catalog titles or IDs are changed. */
internal fun normalizePhoneSearch(text: String): String = Normalizer
    .normalize(text, Normalizer.Form.NFD)
    .replace(searchMarks, "")
    .lowercase(Locale.ROOT)
    .trim()
    .replace(searchWhitespace, " ")

/** Index channels independently: progressive M3U metadata must not renormalize every channel. */
internal class PhoneMediaSearchIndex(media: List<MediaItem>) {
    private val entries = media.distinctBy(MediaItem::id).map { item ->
        item to (listOf(item.title) + item.genres).map(::normalizePhoneSearch)
    }
    fun matches(query: String): List<MediaItem> = searchEntries(entries, query)
}

internal class PhoneChannelSearchIndex(channels: List<LiveChannel>) {
    private val entries = channels.distinctBy(LiveChannel::id).map { channel ->
        channel to listOf(channel.name, channel.category).map(::normalizePhoneSearch)
    }
    fun matches(query: String): List<LiveChannel> = searchEntries(entries, query)
}

private fun <T> searchEntries(entries: List<Pair<T, List<String>>>, query: String): List<T> {
    val term = normalizePhoneSearch(query)
    if (term.isEmpty()) return emptyList()
    return entries.asSequence()
        .filter { (_, fields) -> fields.any { term in it } }
        .map { (item, _) -> item }
        .toList()
}

/** Preserve all matching M3U identities and verified aliases when adding real TMDb results. */
internal fun phoneSearchMedia(index: PhoneMediaSearchIndex, query: String, remote: List<MediaItem>): List<MediaItem> =
    if (normalizePhoneSearch(query).isEmpty()) emptyList() else mergeMediaCatalog(index.matches(query), remote)

/** Interleave the two actual TMDb popularity lists without claiming a combined search ranking. */
internal fun phonePopularTitles(movies: List<MediaItem>, series: List<MediaItem>): List<MediaItem> = buildList {
    for (index in 0 until maxOf(movies.size, series.size)) {
        movies.getOrNull(index)?.let(::add)
        series.getOrNull(index)?.let(::add)
    }
}.distinctBy(MediaItem::id)

/** Prefer the original, playable M3U identity when a TMDb result has a verified match. */
internal fun phoneSearchSuggestions(playlist: MediaPlaylist, popular: List<MediaItem>): List<MediaItem> {
    if (popular.isEmpty()) return playlist.items.distinctBy(MediaItem::id)
    return popular.map { remote ->
        val local = playlist.item(remote.id)
        if (local == null) remote else mergeMediaCatalog(listOf(local), listOf(remote)).first()
    }.distinctBy(MediaItem::id)
}

internal data class PhoneSearchSuggestion(val position: Int, val item: MediaItem)

/** Cycle through distinct, real titles; numbers indicate display position, not search statistics. */
internal fun phoneSuggestionPage(items: List<MediaItem>, page: Int): List<PhoneSearchSuggestion> {
    val unique = items.distinctBy(MediaItem::id)
    if (unique.isEmpty()) return emptyList()
    val pageCount = (unique.size + PHONE_SEARCH_SUGGESTIONS_PER_PAGE - 1) / PHONE_SEARCH_SUGGESTIONS_PER_PAGE
    val remainder = page % pageCount
    val safePage = if (remainder < 0) remainder + pageCount else remainder
    val start = safePage * PHONE_SEARCH_SUGGESTIONS_PER_PAGE
    return unique.drop(start).take(PHONE_SEARCH_SUGGESTIONS_PER_PAGE).mapIndexed { index, item ->
        PhoneSearchSuggestion(start + index + 1, item)
    }
}
