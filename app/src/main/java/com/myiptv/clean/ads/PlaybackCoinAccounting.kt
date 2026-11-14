package com.myiptv.clean.ads

/**
 * The embed bot only counts time that the accessible video actually advanced. Seeking far
 * forward and a stalled/paused video do not count; playback cannot earn faster than real time.
 */
internal fun observedWebPlaybackMs(previousMs: Long?, currentMs: Long?, elapsedMs: Long): Long {
    if (previousMs == null || currentMs == null || elapsedMs <= 0L) return 0L
    val advanced = currentMs - previousMs
    if (advanced <= 0L || advanced > elapsedMs + 1_500L) return 0L
    return minOf(advanced, elapsedMs)
}
