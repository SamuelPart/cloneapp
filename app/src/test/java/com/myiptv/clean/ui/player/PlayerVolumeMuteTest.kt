package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVolumeMuteTest {
    @Test fun mutesOnlyThePlayerAndRestoresItsPreviousVolume() {
        var volume = 67
        val mute = PlayerVolumeMute({ volume }, { volume = it; 0 })
        assertTrue(mute.setMuted(true))
        assertEquals(0, volume)
        assertTrue(mute.setMuted(true)) // reapply after an audio-output restart
        assertTrue(mute.setMuted(false))
        assertEquals(67, volume)
        assertTrue(mute.setMuted(false))
        assertEquals(67, volume)
    }

    @Test fun waitsForAudioAndDoesNotClaimSuccessOnFailure() {
        var volume = -1
        var fails = true
        val mute = PlayerVolumeMute({ volume }, { if (fails) -1 else { volume = it; 0 } })
        assertFalse(mute.setMuted(true))
        volume = 80
        assertFalse(mute.setMuted(true))
        assertEquals(80, volume)
        fails = false
        assertTrue(mute.setMuted(true))
        fails = true
        assertFalse(mute.setMuted(false))
        assertEquals(0, volume)
        fails = false
        assertTrue(mute.setMuted(false))
        assertEquals(80, volume)
    }
}
