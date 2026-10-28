package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaKind

/** A new title, or new numbered episodes of an already existing series. */
internal data class CatalogAddition(
    val mediaId: String,
    val title: String,
    val kind: MediaKind,
    val season: Int? = null,
    val episodes: List<Int> = emptyList()
) {
    val key: String get() = "$mediaId|${season?.toString() ?: "title"}"
}

internal data class StoredCatalog(
    val content: String,
    val pending: List<CatalogAddition> = emptyList()
)

/** A malformed, partial or non-M3U download must never replace the last usable catalog. */
internal fun parseRemoteCatalog(text: String): MediaPlaylist? {
    val firstLine = text.removePrefix("\uFEFF").lineSequence().firstOrNull()?.trim() ?: return null
    if (!firstLine.startsWith("#EXTM3U", ignoreCase = true) ||
        (firstLine.length > 7 && !firstLine[7].isWhitespace())) return null
    // A header-only playlist is valid, but a source without an EXTINF is a broken edit. Ignore
    // optional iframe closing/fallback markup; the parser uses only the opening tag's src.
    var awaitingSource = false
    var inIframe = false
    for (line in MediaPlaylistParser.logicalLines(text.removePrefix("\uFEFF"))) {
        when {
            line.isEmpty() -> Unit
            line.startsWith("#EXTINF:", ignoreCase = true) -> { awaitingSource = true; inIframe = false }
            line.startsWith("#EXTINF", ignoreCase = true) -> return null
            line.startsWith('#') -> Unit
            awaitingSource -> {
                awaitingSource = false
                inIframe = line.startsWith("<iframe", ignoreCase = true) && !line.contains("</iframe", ignoreCase = true)
            }
            inIframe -> if (line.contains("</iframe", ignoreCase = true)) inIframe = false
            else -> return null
        }
    }
    val playlist = MediaPlaylistParser.parse(text)
    return playlist.takeIf { it.ignoredEntries == 0 }
}

/** Diff by the original playlist identity, never by posters, TMDb metadata or changed URLs. */
internal fun catalogAdditions(previous: MediaPlaylist, current: MediaPlaylist): List<CatalogAddition> {
    val result = mutableListOf<CatalogAddition>()
    for (item in current.items) {
        val old = previous.item(item.id) ?: item.tmdbId?.let { tmdbId ->
            item.tmdbMediaType?.let { type -> previous.item("tmdb_${type}_$tmdbId") }
        }
        if (old == null || old.kind != item.kind) {
            result += CatalogAddition(item.id, item.title, item.kind)
            continue // One series notification is enough when its initial episodes arrive together.
        }
        if (item.kind != MediaKind.SERIES) continue
        for (season in current.seasons(item.id)) {
            val oldNumbers = previous.episodes(old.id, season).map { it.number }.toSet()
            val added = current.episodes(item.id, season).map { it.number }.filterNot { it in oldNumbers }.distinct().sorted()
            if (added.isNotEmpty()) result += CatalogAddition(item.id, item.title, item.kind, season, added)
        }
    }
    return result
}

/** Keep unnotified additions through network or permission failures and discard removed content. */
internal fun updateStoredCatalog(previous: StoredCatalog?, nextText: String): StoredCatalog? {
    val next = parseRemoteCatalog(nextText) ?: return null
    val old = previous?.content?.let(::parseRemoteCatalog)
    val fresh = if (old != null) catalogAdditions(old, next) else emptyList()
    val stillAvailable = previous?.pending.orEmpty().mapNotNull { addition ->
        val item = next.item(addition.mediaId) ?: return@mapNotNull null
        if (item.kind != addition.kind) return@mapNotNull null
        if (addition.season == null) addition.copy(mediaId = item.id, title = item.title)
        else {
            val available = next.episodes(item.id, addition.season).map { it.number }.toSet()
            val episodes = addition.episodes.filter { it in available }.distinct().sorted()
            if (episodes.isEmpty()) null else addition.copy(mediaId = item.id, title = item.title, episodes = episodes)
        }
    }

    val merged = linkedMapOf<String, CatalogAddition>()
    for (addition in stillAvailable + fresh) {
        val existing = merged[addition.key]
        merged[addition.key] = if (existing == null || addition.season == null) addition else addition.copy(
            episodes = (existing.episodes + addition.episodes).distinct().sorted()
        )
    }
    val newTitles = merged.values.filter { it.season == null }.map { it.mediaId }.toSet()
    return StoredCatalog(nextText, merged.values.filterNot { it.season != null && it.mediaId in newTitles })
}

internal fun catalogAdditionLabel(addition: CatalogAddition): String {
    val title = addition.title.replace(Regex("[\\p{Cntrl}]+"), " ").trim().take(100)
    if (addition.season == null) {
        return (if (addition.kind == MediaKind.MOVIE) "Nueva película: " else "Nueva serie: ") + title
    }
    val episodes = addition.episodes.take(6).joinToString(", ") { "E$it" }
    val remaining = addition.episodes.size - 6
    return "$title · T${addition.season} $episodes" + if (remaining > 0) " y $remaining más" else ""
}
