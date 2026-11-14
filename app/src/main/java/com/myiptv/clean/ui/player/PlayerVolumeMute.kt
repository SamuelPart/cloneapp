package com.myiptv.clean.ui.player

/** Mute only this VLC player, never the device-wide media volume. */
internal class PlayerVolumeMute(private val readVolume: () -> Int, private val setVolume: (Int) -> Int) {
    private var previousVolume: Int? = null

    fun setMuted(muted: Boolean, requirePositiveVolume: Boolean = false): Boolean {
        if (muted) {
            // LibVLC can report -1 until an audio output exists; leave the request pending.
            // A Shorts preload must also wait through transient zero-volume initialization, or
            // that zero would be restored when the clip becomes active and remain silent.
            val minimum = if (requirePositiveVolume) 1 else 0
            val before = previousVolume ?: readVolume().takeIf { it >= minimum } ?: return false
            if (setVolume(0) != 0) return false
            previousVolume = before
            return true
        }
        val restore = previousVolume ?: return true
        if (setVolume(restore) != 0) return false
        previousVolume = null
        return true
    }
}
