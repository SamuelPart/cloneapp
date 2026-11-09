package com.myiptv.clean.ui.player

/** Mute only this VLC player, never the device-wide media volume. */
internal class PlayerVolumeMute(private val readVolume: () -> Int, private val setVolume: (Int) -> Int) {
    private var previousVolume: Int? = null

    fun setMuted(muted: Boolean): Boolean {
        if (muted) {
            // LibVLC can report -1 until an audio output exists; leave the request pending.
            val before = previousVolume ?: readVolume().takeIf { it >= 0 } ?: return false
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
