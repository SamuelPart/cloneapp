package com.myiptv.clean.data.repository

import android.content.Context
import com.myiptv.clean.ui.compose.MediaItem
import com.myiptv.clean.ui.compose.MediaPlaybackType
import org.json.JSONObject
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/** A playable, direct-source item from the local M3U catalog. */
data class ShortsCandidate(
    val media: MediaItem,
    val playbackId: String,
    val source: PlaylistSource
)

data class ShortClip(
    val candidate: ShortsCandidate,
    val durationMs: Long,
    val startAtMs: Long?
)

/** Storage boundary kept small so the local profile can later be replaced by Supabase. */
interface ShortsPreferenceStore {
    fun genreCounts(): Map<String, Int>
    fun recordWatch(genres: List<String>)
}

class LocalShortsPreferenceStore(context: Context) : ShortsPreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun genreCounts(): Map<String, Int> {
        val json = runCatching { JSONObject(preferences.getString(GENRE_COUNTS, "{}").orEmpty()) }.getOrNull()
            ?: return emptyMap()
        return json.keys().asSequence().mapNotNull { key ->
            normalizeGenre(key)?.let { it to json.optInt(key, 0).coerceAtLeast(0) }
        }.toMap()
    }

    override fun recordWatch(genres: List<String>) {
        val next = genreCounts().toMutableMap()
        genres.mapNotNull(::normalizeGenre).distinct().forEach { genre ->
            next[genre] = (next[genre] ?: 0).plus(1).coerceAtMost(MAX_GENRE_PLAYS)
        }
        val json = JSONObject().apply { next.forEach { (genre, count) -> put(genre, count) } }
        preferences.edit().putString(GENRE_COUNTS, json.toString()).apply()
    }

    private companion object {
        const val PREFERENCES = "samplay_shorts_profile"
        const val GENRE_COUNTS = "genre_counts"
        const val MAX_GENRE_PLAYS = 100_000
    }
}

/** Build candidates only from the active M3U catalog. Iframes are skipped because their seek and
 * duration controls belong to another site, so we cannot promise a 1:30–2:30 excerpt. */
fun shortsCandidates(playlist: MediaPlaylist): List<ShortsCandidate> = buildList {
    playlist.sources.forEach { (playbackId, source) ->
        if (source.type != MediaPlaybackType.DIRECT) return@forEach
        val mediaId = parseEpisodeSourceId(playbackId)?.mediaId ?: playbackId
        val media = playlist.item(mediaId) ?: return@forEach
        add(ShortsCandidate(media = media, playbackId = playbackId, source = source))
    }
}.distinctBy(ShortsCandidate::playbackId)

/**
 * Produce an unordered recommendation batch. Watched genres increase their probability while
 * every other title keeps a non-zero exploration chance. Sampling with replacement lets one film
 * contribute several distinct excerpts in a batch; repeat appearances are gently down-weighted.
 * A fresh clip length and start point are selected for every excerpt.
 */
fun recommendShortClips(
    candidates: List<ShortsCandidate>,
    genreCounts: Map<String, Int>,
    random: Random = Random.Default,
    batchSize: Int = candidates.size
): List<ShortClip> {
    val allCandidates = candidates.distinctBy(ShortsCandidate::playbackId)
    if (allCandidates.isEmpty() || batchSize <= 0) return emptyList()
    val appearancesByWork = mutableMapOf<String, Int>()
    val result = ArrayList<ShortClip>(batchSize)
    while (result.size < batchSize) {
        val weights = allCandidates.map { candidate ->
            val workId = shortsWorkId(candidate.media)
            recommendationWeight(shortsTasteLabels(candidate.media), genreCounts) /
                sqrt(1.0 + (appearancesByWork[workId] ?: 0).toDouble())
        }
        val total = weights.sum()
        var target = random.nextDouble() * total
        val selectedIndex = weights.indexOfFirst { weight ->
            target -= weight
            target < 0.0
        }.let { if (it < 0) allCandidates.lastIndex else it }
        val candidate = allCandidates[selectedIndex]
        val workId = shortsWorkId(candidate.media)
        val previousClipsForWork = result.filter { shortsWorkId(it.candidate.media) == workId }
        var clipDuration = random.nextLong(MIN_CLIP_MS, MAX_CLIP_MS + 1)
        var start = randomStart(candidate, clipDuration, random)
        var attempt = 0
        while (
            previousClipsForWork.any { previous ->
                (start != null && previous.startAtMs == start) ||
                    (start == null && previous.startAtMs == null && previous.durationMs == clipDuration)
            } && attempt < 16
        ) {
            clipDuration = random.nextLong(MIN_CLIP_MS, MAX_CLIP_MS + 1)
            start = randomStart(candidate, clipDuration, random)
            attempt++
        }
        result += ShortClip(candidate, clipDuration, start)
        appearancesByWork[workId] = (appearancesByWork[workId] ?: 0) + 1
    }
    return result
}

private fun randomStart(candidate: ShortsCandidate, clipDurationMs: Long, random: Random): Long? {
    val runtime = parseRuntimeMs(candidate.media.duration) ?: return null
    val maxStart = (runtime - clipDurationMs).coerceAtLeast(0L)
    return if (maxStart > 0L) random.nextLong(maxStart + 1L) else 0L
}

private fun shortsWorkId(media: MediaItem): String = media.tmdbId?.takeIf { it > 0L }
    ?.let { "${media.kind.name.lowercase()}_$it" }
    ?: media.id

fun shortsTasteLabels(media: MediaItem): List<String> = media.genres + when (media.kind) {
    com.myiptv.clean.ui.compose.MediaKind.MOVIE -> "Película"
    com.myiptv.clean.ui.compose.MediaKind.SERIES -> "Serie"
}

fun recommendationWeight(genres: List<String>, genreCounts: Map<String, Int>): Double {
    val affinities = genres.mapNotNull(::normalizeGenre).distinct().map { genre ->
        ln(1.0 + (genreCounts[genre] ?: 0).coerceAtLeast(0).toDouble())
    }
    // Exploration remains possible even for genres with no recorded watches.
    val averageAffinity = if (affinities.isEmpty()) 0.0 else affinities.average()
    return 1.0 + averageAffinity * 0.8
}

fun parseRuntimeMs(value: String): Long? {
    val text = value.trim().lowercase()
    if (text.isBlank() || text == "—") return null
    val hourValues = Regex("(\\d+)\\s*(?:h|hr|hrs|hour|hours)").findAll(text).mapNotNull { it.groupValues[1].toLongOrNull() }.toList()
    val minuteValues = Regex("(\\d+)\\s*(?:m|min|mins|minute|minutes)").findAll(text).mapNotNull { it.groupValues[1].toLongOrNull() }.toList()
    val secondsValues = Regex("(\\d+)\\s*(?:s|sec|secs|second|seconds)").findAll(text).mapNotNull { it.groupValues[1].toLongOrNull() }.toList()
    val numeric = Regex("^\\d+$").matchEntire(text)?.value?.toLongOrNull()
    val totalMinutes = hourValues.sum() * 60L + minuteValues.sum() + (numeric ?: 0L)
    val totalSeconds = totalMinutes * 60L + secondsValues.sum()
    return totalSeconds.takeIf { it > 0L }?.times(1_000L)
}

private fun normalizeGenre(value: String): String? = value.trim().lowercase().takeIf(String::isNotBlank)

const val SHORTS_BATCH_SIZE = 30
private const val MIN_CLIP_MS = 90_000L
private const val MAX_CLIP_MS = 150_000L
