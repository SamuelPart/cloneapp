package com.myiptv.clean.ui.player

import java.util.Locale

/** On mobile the first tap on hidden controls only reveals them; TV keeps its center action. */
internal fun shouldToggleOnSurfaceTap(isTv: Boolean, controlsVisible: Boolean, isCenter: Boolean): Boolean =
    isCenter && (isTv || controlsVisible)

/** Mobile controls hide even while paused or when a web provider cannot report playback state. */
internal fun shouldAutoHideControls(isTv: Boolean, controlsVisible: Boolean, tvPlaybackAllowsHiding: Boolean, blocked: Boolean): Boolean =
    controlsVisible && !blocked && (!isTv || tvPlaybackAllowsHiding)

/** Touching the middle changes playback; taps at the edges only reveal controls. */
internal fun isPlaybackCenter(x: Float, y: Float, width: Int, height: Int): Boolean =
    width > 0 && height > 0 && x >= width * 0.2f && x <= width * 0.8f &&
        y >= height * 0.2f && y <= height * 0.8f

internal enum class CenterPlaybackAction { RETRY, RESTART, PAUSE, PLAY }

internal fun centerPlaybackAction(hasError: Boolean, isEnded: Boolean, wantsToPlay: Boolean): CenterPlaybackAction = when {
    hasError -> CenterPlaybackAction.RETRY
    isEnded -> CenterPlaybackAction.RESTART
    wantsToPlay -> CenterPlaybackAction.PAUSE
    else -> CenterPlaybackAction.PLAY
}

internal data class PlayerControlSizes(val side: Float, val center: Float, val spacing: Float)

/** Three controls with at least 44dp touch targets, including on narrow phones. */
internal fun playerControlSizes(screenWidthDp: Float, isTv: Boolean): PlayerControlSizes = when {
    isTv -> PlayerControlSizes(side = 58f, center = 72f, spacing = 24f)
    screenWidthDp < 320f -> PlayerControlSizes(side = 44f, center = 56f, spacing = 10f)
    else -> PlayerControlSizes(side = 48f, center = 64f, spacing = 16f)
}

internal fun formatPlaybackTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remainder = seconds % 60L
    return if (hours > 0L) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainder)
    else String.format(Locale.ROOT, "%02d:%02d", minutes, remainder)
}

/** Apply only the two HTTP headers supplied by the channel catalog, never arbitrary VLC options. */
internal fun vlcMediaOptions(userAgent: String?, referrer: String?): List<String> {
    fun safeHeader(value: String?, maxLength: Int): String? = value?.trim()?.takeIf { header ->
        header.isNotEmpty() && header.length <= maxLength && header.none { it.code < 0x20 || it.code == 0x7f }
    }

    val options = mutableListOf(":http-user-agent=${safeHeader(userAgent, 512) ?: "SamPlay/1.0"}")
    safeHeader(referrer, 2_048)?.let { options.add(":http-referrer=$it") }
    return options
}
