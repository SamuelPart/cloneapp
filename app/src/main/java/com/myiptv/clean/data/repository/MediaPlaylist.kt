package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaEpisode
import com.myiptv.clean.ui.compose.MediaItem
import com.myiptv.clean.ui.compose.MediaKind
import com.myiptv.clean.ui.compose.MediaPlaybackType
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/** Playback sources are supplied by the owner of the packaged playlist, not by TMDb. */
data class PlaylistSource(val url: String, val type: MediaPlaybackType)

/** The new catalog takes precedence while old on-device sources remain usable as a fallback. */
fun resolvePlaybackSource(
    catalog: PlaylistSource?,
    legacy: PlaylistSource?,
    fallback: PlaylistSource? = null
): PlaylistSource? = listOfNotNull(catalog, legacy, fallback).firstOrNull { it.url.isNotBlank() }

data class MediaPlaylist(
    val items: List<MediaItem> = emptyList(),
    val sources: Map<String, PlaylistSource> = emptyMap(),
    val episodes: Map<String, Map<Int, List<MediaEpisode>>> = emptyMap(),
    val ignoredEntries: Int = 0,
    val loaded: Boolean = true,
    val errorMessage: String? = null
) {
    private val itemsById = items.associateBy(MediaItem::id)
    // Only verified/explicit TMDb identities become aliases. Keep the first M3U source if the
    // playlist intentionally contains the same work more than once.
    private val itemsByTmdbId = items.mapNotNull { item ->
        tmdbIdentity(item)?.let { (type, id) -> "tmdb_${type}_$id" to item }
    }.asReversed().toMap()

    fun item(id: String): MediaItem? = itemsById[id] ?: itemsByTmdbId[id]
    fun canonicalMediaId(id: String): String = item(id)?.id ?: id
    fun mediaAliases(id: String): Set<String> {
        val media = item(id) ?: return setOf(id)
        val tmdbId = tmdbIdentity(media)?.let { (type, number) -> "tmdb_${type}_$number" }
        return setOfNotNull(media.id, tmdbId)
    }
    fun expandedMediaIds(ids: Set<String>): Set<String> = ids.flatMapTo(mutableSetOf(), ::mediaAliases)

    /** Includes a previously saved TMDb source when this playlist uses an m3u_* identity. */
    fun sourceAliases(id: String): Set<String> {
        val episode = parseEpisodeSourceId(id)
        val aliases = if (episode == null) mediaAliases(id)
        else mediaAliases(episode.mediaId).mapTo(mutableSetOf()) {
            episodeSourceId(it, episode.seasonNumber, episode.episodeNumber)
        }
        return linkedSetOf(id).apply { addAll(aliases) }
    }

    fun source(id: String): PlaylistSource? {
        sources[id]?.let { return it }
        val episode = parseEpisodeSourceId(id)
        val canonical = if (episode != null) {
            episodeSourceId(canonicalMediaId(episode.mediaId), episode.seasonNumber, episode.episodeNumber)
        } else canonicalMediaId(id)
        return sources[canonical]
    }
    fun seasons(id: String): List<Int> = episodes[canonicalMediaId(id)].orEmpty().keys.sorted()
    fun episodes(id: String, season: Int): List<MediaEpisode> = episodes[canonicalMediaId(id)]?.get(season).orEmpty()

    val statusMessage: String
        get() = when {
            !loaded -> "Cargando catálogo M3U…"
            errorMessage != null -> errorMessage
            sources.isEmpty() && ignoredEntries > 0 -> "El catálogo M3U no contiene entradas válidas; revisa su formato."
            sources.isEmpty() -> "El catálogo M3U está vacío."
            ignoredEntries > 0 -> "${sources.size} ${if (sources.size == 1) "enlace M3U cargado" else "enlaces M3U cargados"}; $ignoredEntries ${if (ignoredEntries == 1) "entrada ignorada" else "entradas ignoradas"}."
            else -> "${sources.size} ${if (sources.size == 1) "enlace cargado" else "enlaces cargados"} desde el catálogo M3U."
        }

    companion object {
        val Loading = MediaPlaylist(loaded = false)
    }
}

/** Keep the IDs used by details and the player identical for both TMDb and playlist-only series. */
fun episodeSourceId(mediaId: String, season: Int, episode: Int): String = "${mediaId}_s${season}_e${episode}"

data class EpisodeSourceReference(val mediaId: String, val seasonNumber: Int, val episodeNumber: Int)

fun parseEpisodeSourceId(value: String): EpisodeSourceReference? {
    val match = Regex("""^((?:tmdb_(?:movie|tv)_\d+|m3u_tv_[a-f0-9]+))_s(\d+)_e(\d+)$""").matchEntire(value) ?: return null
    val season = match.groupValues[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val episode = match.groupValues[3].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return EpisodeSourceReference(match.groupValues[1], season, episode)
}

/** Enrich only an exact ID match; keep supplied artwork and fields when TMDb has none. */
fun mergeMediaMetadata(local: MediaItem, remote: MediaItem): MediaItem {
    require(local.id == remote.id)
    return remote.copy(
        title = remote.title.takeUnless { it.isBlank() || it == "Sin título" } ?: local.title,
        year = remote.year.takeIf { it > 0 } ?: local.year,
        genres = remote.genres.ifEmpty { local.genres },
        synopsis = remote.synopsis.takeUnless { it.isBlank() || it == "Sinopsis no disponible." } ?: local.synopsis,
        duration = remote.duration.takeUnless { it.isBlank() || it == "—" } ?: local.duration,
        rating = remote.rating.takeIf { it > 0.0 } ?: local.rating,
        streamUrl = local.streamUrl ?: remote.streamUrl,
        playbackType = if (local.streamUrl != null) local.playbackType else remote.playbackType,
        posterUrl = remote.posterUrl?.takeIf(String::isNotBlank) ?: local.posterUrl,
        backdropUrl = remote.backdropUrl?.takeIf(String::isNotBlank) ?: local.backdropUrl,
        tmdbId = remote.tmdbId ?: local.tmdbId,
        tmdbMediaType = remote.tmdbMediaType ?: local.tmdbMediaType,
        castNames = remote.castNames.ifEmpty { local.castNames },
        seasonCount = remote.seasonCount ?: local.seasonCount,
        episodeCount = remote.episodeCount ?: local.episodeCount
    )
}

/** A verified TMDb match adds metadata without changing the M3U identity or playback links. */
internal fun mergePlaylistTmdbMetadata(local: MediaItem, remote: MediaItem): MediaItem {
    require(local.kind == remote.kind && remote.tmdbId != null && remote.tmdbId > 0)
    require(remote.tmdbMediaType == if (local.kind == MediaKind.MOVIE) "movie" else "tv")
    require(local.tmdbId == null || local.tmdbId == remote.tmdbId)
    return mergeMediaMetadata(local, remote.copy(id = local.id))
}

private fun tmdbIdentity(item: MediaItem): Pair<String, Long>? {
    val type = if (item.kind == MediaKind.MOVIE) "movie" else "tv"
    val id = item.tmdbId?.takeIf { it > 0 } ?: return null
    return if (item.tmdbMediaType == type) type to id else null
}

/** Keep playlist IDs and sources; suppress a duplicate remote card only after an ID is verified. */
fun mergeMediaCatalog(playlistItems: List<MediaItem>, tmdbItems: List<MediaItem>): List<MediaItem> {
    val byId = tmdbItems.associateBy(MediaItem::id)
    val byTmdb = tmdbItems.mapNotNull { item -> tmdbIdentity(item)?.let { it to item } }.toMap()
    val playlistTmdbIds = playlistItems.mapNotNull(::tmdbIdentity).toSet()
    val enriched = playlistItems.map { local ->
        val remote = byId[local.id] ?: tmdbIdentity(local)?.let(byTmdb::get)
        when {
            remote == null -> local
            local.id == remote.id -> mergeMediaMetadata(local, remote)
            else -> mergePlaylistTmdbMetadata(local, remote)
        }
    }
    val remaining = tmdbItems.filter { tmdbIdentity(it) !in playlistTmdbIds }
    return (enriched + remaining).distinctBy(MediaItem::id)
}

fun mergeSeasonEpisodes(local: List<MediaEpisode>, remote: List<MediaEpisode>): List<MediaEpisode> {
    val supplied = local.associateBy(MediaEpisode::number)
    val enriched = remote.map { episode ->
        val fallback = supplied[episode.number]
        if (fallback == null) episode else episode.copy(
            synopsis = episode.synopsis.takeUnless { it.isBlank() || it == "Sinopsis no disponible." } ?: fallback.synopsis,
            duration = episode.duration.takeUnless { it.isBlank() || it == "—" } ?: fallback.duration,
            stillUrl = episode.stillUrl?.takeIf(String::isNotBlank) ?: fallback.stillUrl
        )
    }
    return (enriched + local).distinctBy(MediaEpisode::number).sortedBy(MediaEpisode::number)
}

/**
 * Reads EXTINF entries supplied by the app owner. TMDb IDs are optional: metadata lookup is
 * separate from parsing and never changes the playlist's source IDs or URLs.
 */
object MediaPlaylistParser {
    private val attributes = Regex("""([A-Za-z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    private val tmdbIdentity = Regex("""^tmdb_(movie|tv)_(\d+)(?:_s(\d+)_e(\d+))?$""", RegexOption.IGNORE_CASE)
    private val iframeSource = Regex("""(?i)(?:^|[\s<])src\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))""")
    private val seasonGroup = Regex("""^(.+?)\s+temporada\s+(\d+)\s*$""", RegexOption.IGNORE_CASE)
    private val numberedEpisode = Regex("""^(?:cap[ií]tulo|episodio|episode|chapter)\s+(\d+)(?:\s*[-:.]\s*.*)?$""", RegexOption.IGNORE_CASE)
    private val movieYear = Regex("""\s+\(((?:19|20)\d{2})\)\s*$""")
    private val markdownLink = Regex("""^\[[^]]*]\((https?://\S+)\)$""", RegexOption.IGNORE_CASE)
    private val directMediaPath = Regex("""\.(?:m3u8|mpd|mp4|m4v|mkv|webm|avi|mov|ts|m2ts|mpeg|mpg|aac|mp3|flac|ogg|ogv)$""", RegexOption.IGNORE_CASE)
    private val webPlayerPath = Regex("""/(?:watch|embed|iframe)(?:/|$)|\.html?$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): MediaPlaylist {
        val titles = linkedMapOf<String, MediaItem>()
        val sources = linkedMapOf<String, PlaylistSource>()
        val episodeLists = linkedMapOf<String, MutableMap<Int, MutableMap<Int, MediaEpisode>>>()
        var pending: Metadata? = null
        var ignored = 0

        for (line in logicalLines(text.removePrefix("\uFEFF"))) {
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    if (pending != null) ignored++
                    pending = parseMetadata(line)
                    if (pending == null) ignored++
                }
                line.startsWith('#') -> Unit
                else -> {
                    val metadata = pending ?: continue
                    pending = null
                    val source = parseSource(line, metadata.playbackType)
                    if (source == null) {
                        ignored++
                        continue
                    }
                    val previous = titles[metadata.mediaId]
                    if (previous?.tmdbId != null && metadata.tmdbId != null && previous.tmdbId != metadata.tmdbId) {
                        ignored++ // Never attach conflicting explicit identities to the same series.
                        continue
                    }
                    val entryId = if (metadata.season != null && metadata.episode != null) {
                        episodeSourceId(metadata.mediaId, metadata.season, metadata.episode)
                    } else metadata.mediaId
                    if (entryId !in sources) sources[entryId] = source
                    if (previous == null) titles[metadata.mediaId] = metadata.toMediaItem()
                    else if (previous.tmdbId == null && metadata.tmdbId != null) {
                        titles[metadata.mediaId] = previous.copy(
                            tmdbId = metadata.tmdbId,
                            tmdbMediaType = if (previous.kind == MediaKind.MOVIE) "movie" else "tv"
                        )
                    }

                    if (metadata.season != null && metadata.episode != null) {
                        val season = episodeLists.getOrPut(metadata.mediaId) { linkedMapOf() }
                            .getOrPut(metadata.season) { linkedMapOf() }
                        if (metadata.episode !in season) {
                            season[metadata.episode] = MediaEpisode(
                                number = metadata.episode,
                                title = metadata.episodeTitle,
                                duration = metadata.duration,
                                synopsis = metadata.description,
                                stillUrl = metadata.imageUrl
                            )
                        }
                    }
                }
            }
        }
        if (pending != null) ignored++

        val episodes = episodeLists.mapValues { (_, seasons) ->
            seasons.mapValues { (_, items) -> items.toSortedMap().values.toList() }
        }
        val items = titles.values.map { item ->
            val seasons = episodes[item.id].orEmpty()
            if (item.kind == MediaKind.SERIES && seasons.isNotEmpty()) {
                item.copy(seasonCount = seasons.size, episodeCount = seasons.values.sumOf(List<MediaEpisode>::size))
            } else item
        }
        return MediaPlaylist(items = items, sources = sources, episodes = episodes, ignoredEntries = ignored)
    }

    /** An iframe's opening tag may be pasted across several physical lines. */
    internal fun logicalLines(text: String): List<String> {
        val result = mutableListOf<String>()
        var iframe: StringBuilder? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            val pendingIframe = iframe
            if (pendingIframe != null) {
                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    result.add(pendingIframe.toString())
                    iframe = null
                    result.add(line)
                } else {
                    pendingIframe.append(' ').append(line)
                    if (line.contains('>')) {
                        result.add(pendingIframe.toString())
                        iframe = null
                    }
                }
            } else if (line.startsWith("<iframe", ignoreCase = true) && !line.contains('>')) {
                iframe = StringBuilder(line)
            } else {
                result.add(line)
            }
        }
        iframe?.let { result.add(it.toString()) }
        return result
    }

    private fun parseMetadata(line: String): Metadata? {
        val separator = unquotedComma(line)
        if (separator < 0) return null
        val values = attributes.findAll(line.substring(0, separator)).associate { match ->
            match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues.drop(2).firstOrNull(String::isNotEmpty).orEmpty().trim()
        }
        val displayName = line.substring(separator + 1).trim().ifBlank { values["tvg-name"].orEmpty() }
        if (displayName.isBlank()) return null
        val rawId = values["tvg-id"].orEmpty().ifBlank { values["media-id"].orEmpty() }
        val tmdbMatch = tmdbIdentity.matchEntire(rawId)
        if (rawId.startsWith("tmdb_", ignoreCase = true) && tmdbMatch == null) return null
        val idKind = when (tmdbMatch?.groupValues?.get(1)?.lowercase(Locale.ROOT)) {
            "movie" -> MediaKind.MOVIE
            "tv" -> MediaKind.SERIES
            else -> null
        }
        val declaredKind = kindFrom(values["media-type"] ?: values["content-type"] ?: values["kind"])
        if (idKind != null && declaredKind != null && idKind != declaredKind) return null
        val groupMatch = seasonGroup.matchEntire(values["group-title"].orEmpty().trim())
        val groupSeason = groupMatch?.groupValues?.get(2)?.toIntOrNull()
        if (groupMatch != null && groupSeason == null) return null
        val groupSeriesTitle = groupMatch?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
        val groupEpisodeMatch = if (groupMatch != null) numberedEpisode.matchEntire(displayName) else null
        val groupEpisode = groupEpisodeMatch?.groupValues?.get(1)?.toIntOrNull()
        if (groupEpisodeMatch != null && groupEpisode == null) return null
        val kind = idKind ?: declaredKind ?: kindFrom(values["group-title"])
            ?: if (groupMatch != null && groupEpisode != null) MediaKind.SERIES else return null
        if (groupMatch != null && kind != MediaKind.SERIES) return null

        val idSeasonText = tmdbMatch?.groupValues?.get(3)?.takeIf(String::isNotBlank)
        val idEpisodeText = tmdbMatch?.groupValues?.get(4)?.takeIf(String::isNotBlank)
        val idSeason = idSeasonText?.toIntOrNull()
        val idEpisode = idEpisodeText?.toIntOrNull()
        if ((idSeasonText != null && idSeason == null) || (idEpisodeText != null && idEpisode == null)) return null
        val seasonValue = values["season"] ?: values["season-number"]
        val episodeValue = values["episode"] ?: values["episode-number"]
        val suppliedSeason = seasonValue?.toIntOrNull()
        val suppliedEpisode = episodeValue?.toIntOrNull()
        if ((seasonValue != null && suppliedSeason == null) || (episodeValue != null && suppliedEpisode == null)) return null
        if ((idSeason != null && suppliedSeason != null && idSeason != suppliedSeason) ||
            (idEpisode != null && suppliedEpisode != null && idEpisode != suppliedEpisode) ||
            (groupSeason != null && idSeason != null && groupSeason != idSeason) ||
            (groupSeason != null && suppliedSeason != null && groupSeason != suppliedSeason) ||
            (groupEpisode != null && idEpisode != null && groupEpisode != idEpisode) ||
            (groupEpisode != null && suppliedEpisode != null && groupEpisode != suppliedEpisode)) return null
        val season = idSeason ?: suppliedSeason ?: groupSeason
        val episode = idEpisode ?: suppliedEpisode ?: groupEpisode
        if ((season == null) != (episode == null) || (season != null && (kind != MediaKind.SERIES || season < 0 || episode!! < 1))) return null

        val idFromTag = tmdbMatch?.groupValues?.get(2)?.toLongOrNull()
        val suppliedTmdb = values["tmdb-id"]?.toLongOrNull()
        if ((tmdbMatch != null && idFromTag == null) || (values["tmdb-id"] != null && suppliedTmdb == null)) return null
        if (idFromTag != null && suppliedTmdb != null && idFromTag != suppliedTmdb) return null
        val numericTmdb = idFromTag ?: suppliedTmdb
        if (numericTmdb != null && numericTmdb <= 0) return null
        // Optional metadata-only ID keeps an existing m3u_* identity and all saved links intact.
        val suppliedMatchId = values["tmdb-match-id"]?.toLongOrNull()
        if (values["tmdb-match-id"] != null && (suppliedMatchId == null || suppliedMatchId <= 0)) return null
        if (numericTmdb != null && suppliedMatchId != null && numericTmdb != suppliedMatchId) return null
        val seriesTitle = values["series-title"].orEmpty().ifBlank { groupSeriesTitle ?: displayName }
        val title = if (season != null) seriesTitle else displayName
        val mediaId = if (numericTmdb != null) {
            "tmdb_${if (kind == MediaKind.MOVIE) "movie" else "tv"}_$numericTmdb"
        } else {
            val identity = if (season != null) values["series-id"].orEmpty().ifBlank { seriesTitle }
            else rawId.ifBlank { displayName }
            // Season-specific group names must not split one series into separate titles.
            val identityGroup = if (groupMatch != null && season != null) "" else values["group-title"].orEmpty()
            val stableKey = listOf(kind.name, identity, values["year"].orEmpty(), identityGroup).joinToString("\u0000")
            "m3u_${if (kind == MediaKind.MOVIE) "movie" else "tv"}_${stableHash(stableKey)}"
        }
        val imageUrl = httpUrl(values["tvg-logo"].orEmpty())
        val backdropUrl = httpUrl(values["tvg-backdrop"].orEmpty())
        val type = when ((values["playback-type"] ?: values["type"]).orEmpty().lowercase(Locale.ROOT)) {
            "iframe", "embed" -> MediaPlaybackType.IFRAME
            "direct", "directo" -> MediaPlaybackType.DIRECT
            "" -> null // Infer obvious web-player URLs; preserve direct playback for ambiguous URLs.
            else -> return null
        }
        return Metadata(
            mediaId = mediaId,
            kind = kind,
            title = title,
            episodeTitle = values["episode-title"].orEmpty().ifBlank { displayName },
            season = season,
            episode = episode,
            tmdbId = numericTmdb ?: suppliedMatchId,
            year = values["year"]?.toIntOrNull()?.takeIf { it > 0 }
                ?: if (kind == MediaKind.MOVIE) movieYear.find(displayName)?.groupValues?.get(1)?.toIntOrNull() ?: 0 else 0,
            genres = values["genre"].orEmpty().split('|').map(String::trim).filter(String::isNotEmpty),
            description = values["description"].orEmpty().ifBlank { "Sinopsis no disponible." },
            duration = values["duration"].orEmpty().ifBlank { "—" },
            imageUrl = imageUrl,
            backdropUrl = backdropUrl,
            playbackType = type
        )
    }

    private fun parseSource(value: String, declaredType: MediaPlaybackType?): PlaylistSource? {
        val trimmed = value.trim()
        val iframeMarkup = trimmed.startsWith("<iframe", ignoreCase = true)
        val candidate = if (iframeMarkup) {
            iframeSource.find(trimmed)?.groupValues?.drop(1)?.firstOrNull(String::isNotBlank)
                ?.replace("&amp;", "&", ignoreCase = true)
        } else markdownLink.matchEntire(trimmed)?.groupValues?.get(1) ?: trimmed
        val url = httpUrl(candidate.orEmpty()) ?: return null
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
        val inferred = when {
            directMediaPath.containsMatchIn(path) -> MediaPlaybackType.DIRECT
            webPlayerPath.containsMatchIn(path) -> MediaPlaybackType.IFRAME
            else -> MediaPlaybackType.DIRECT
        }
        return PlaylistSource(url, if (iframeMarkup) MediaPlaybackType.IFRAME else declaredType ?: inferred)
    }

    private fun kindFrom(value: String?): MediaKind? = when (value?.trim()?.lowercase(Locale.ROOT)) {
        "movie", "movies", "pelicula", "película", "peliculas", "películas" -> MediaKind.MOVIE
        "series", "serie", "tv", "episode", "episodio" -> MediaKind.SERIES
        else -> null
    }

    private fun httpUrl(value: String): String? = runCatching {
        val clean = value.trim()
        val parsed = URI(clean)
        clean.takeIf { parsed.scheme?.lowercase(Locale.ROOT) in setOf("http", "https") && !parsed.host.isNullOrBlank() }
    }.getOrNull()

    private fun stableHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(10).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun unquotedComma(value: String): Int {
        var quote: Char? = null
        for (index in value.indices) {
            val char = value[index]
            if ((char == '"' || char == '\'') && (quote == null || quote == char)) quote = if (quote == null) char else null
            if (char == ',' && quote == null) return index
        }
        return -1
    }

    private data class Metadata(
        val mediaId: String,
        val kind: MediaKind,
        val title: String,
        val episodeTitle: String,
        val season: Int?,
        val episode: Int?,
        val tmdbId: Long?,
        val year: Int,
        val genres: List<String>,
        val description: String,
        val duration: String,
        val imageUrl: String?,
        val backdropUrl: String?,
        val playbackType: MediaPlaybackType?
    ) {
        fun toMediaItem() = MediaItem(
            id = mediaId,
            title = title,
            kind = kind,
            year = year,
            rating = 0.0,
            genres = genres,
            synopsis = description,
            duration = duration,
            colorA = 0xFF173141L,
            colorB = 0xFF101014L,
            posterUrl = imageUrl,
            backdropUrl = backdropUrl,
            tmdbId = tmdbId,
            tmdbMediaType = tmdbId?.let { if (kind == MediaKind.MOVIE) "movie" else "tv" }
        )
    }
}
