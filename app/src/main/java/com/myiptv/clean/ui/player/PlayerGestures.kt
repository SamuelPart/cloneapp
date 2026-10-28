package com.myiptv.clean.ui.player

/** The middle of the video toggles playback; the edges remain available to controls and gestures. */
internal fun isPlaybackCenter(x: Float, y: Float, width: Int, height: Int): Boolean =
    width > 0 && height > 0 && x >= width * 0.2f && x <= width * 0.8f &&
        y >= height * 0.2f && y <= height * 0.8f

/** Fit four side buttons and a larger play button inside narrow phone screens. */
internal fun phonePlayerButtonWidth(screenWidthDp: Float): Float =
    ((screenWidthDp - 24f - 52f) / 4f).coerceIn(28f, 48f)

internal enum class CenterPlaybackAction { PREPARE, RESTART, PAUSE, PLAY }

internal fun centerPlaybackAction(isIdle: Boolean, isEnded: Boolean, playWhenReady: Boolean): CenterPlaybackAction = when {
    isEnded -> CenterPlaybackAction.RESTART
    isIdle -> CenterPlaybackAction.PREPARE
    playWhenReady -> CenterPlaybackAction.PAUSE
    else -> CenterPlaybackAction.PLAY
}
