package com.myiptv.clean.ui.player

/** Results of a second, lightweight HTTP request. It may not have the same permissions as VLC. */
internal enum class ChannelProbeResult { RESPONDING, UNAVAILABLE, INCONCLUSIVE }

internal data class LivePlaybackEvidence(
    val hasStarted: Boolean = false,
    val hasBuffered: Boolean = false,
    val failed: Boolean = false,
    val pausedByUser: Boolean = false
) {
    val hasProgress: Boolean get() = hasStarted || hasBuffered

    fun withEvent(event: PlaybackEvent): LivePlaybackEvidence = when (event) {
        PlaybackEvent.Opening -> if (failed) copy(hasStarted = false, hasBuffered = false, failed = false) else this
        PlaybackEvent.Playing -> copy(hasStarted = true, failed = false)
        is PlaybackEvent.Buffering -> if (event.percent > 0f) copy(hasBuffered = true) else this
        PlaybackEvent.Error, PlaybackEvent.Ended -> copy(failed = true)
        PlaybackEvent.UserPaused -> copy(pausedByUser = true)
        PlaybackEvent.UserResumed -> copy(pausedByUser = false)
        else -> this
    }
}

internal enum class ChannelHealthDecision { WAIT, HEALTHY, RETRY, NEXT }

internal const val CHANNEL_CHECK_MIN_MS = 3_500L

/** Never open a second connection while VLC may still be loading a working single-session stream. */
internal fun shouldProbeLiveChannel(playback: LivePlaybackEvidence): Boolean = playback.failed && !playback.pausedByUser

/**
 * A timeout, a response denied to the separate probe, and a slow VLC buffer are not failures.
 * A terminal VLC event proves only that the current playback attempt stopped. Retry uncertain
 * cases once; if the separate HTTP check is still inconclusive, keep waiting rather than declaring
 * a slow endpoint dead. A 200 alone does not prove the video can actually play.
 */
internal fun channelHealthDecision(
    elapsedMs: Long,
    playback: LivePlaybackEvidence,
    probe: ChannelProbeResult,
    alreadyRetried: Boolean
): ChannelHealthDecision {
    if (playback.pausedByUser) return ChannelHealthDecision.WAIT
    if (!playback.failed && playback.hasProgress) return ChannelHealthDecision.HEALTHY
    if (elapsedMs < CHANNEL_CHECK_MIN_MS) return ChannelHealthDecision.WAIT
    if (!playback.failed) return ChannelHealthDecision.WAIT
    if (!alreadyRetried && (probe != ChannelProbeResult.UNAVAILABLE || playback.hasStarted)) {
        return ChannelHealthDecision.RETRY
    }
    return if (probe == ChannelProbeResult.INCONCLUSIVE) ChannelHealthDecision.WAIT else ChannelHealthDecision.NEXT
}

/** Keep the real list order and do not revisit failures until the viewer manually selects again. */
internal fun nextUnfailedChannelIndex(ids: List<String>, currentId: String, failedIds: Set<String>): Int? {
    if (ids.isEmpty()) return null
    val current = ids.indexOf(currentId).takeIf { it >= 0 } ?: -1
    for (offset in 1..ids.size) {
        val index = (current + offset) % ids.size
        if (ids[index] !in failedIds) return index
    }
    return null
}

internal fun adjacentChannelIndex(size: Int, currentIndex: Int, direction: Int): Int? {
    if (size <= 0 || direction == 0) return null
    val origin = currentIndex.takeIf { it in 0 until size } ?: if (direction > 0) -1 else 0
    val delta = if (direction > 0) 1 else -1
    return (origin + delta + size) % size
}
