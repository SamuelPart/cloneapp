package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturesTest {
    @Test
    fun centerAlternatesPlaybackAndCanRestartOrPrepare() {
        assertEquals(CenterPlaybackAction.PAUSE, centerPlaybackAction(isIdle = false, isEnded = false, playWhenReady = true))
        assertEquals(CenterPlaybackAction.PLAY, centerPlaybackAction(isIdle = false, isEnded = false, playWhenReady = false))
        assertEquals(CenterPlaybackAction.RESTART, centerPlaybackAction(isIdle = false, isEnded = true, playWhenReady = true))
        assertEquals(CenterPlaybackAction.PREPARE, centerPlaybackAction(isIdle = true, isEnded = false, playWhenReady = false))
    }

    @Test
    fun controlsFitCompactPhonesWithoutChangingTheTvSizes() {
        assertEquals(48f, phonePlayerButtonWidth(360f), 0.01f)
        assertEquals(41f, phonePlayerButtonWidth(240f), 0.01f)
        assertEquals(31f, phonePlayerButtonWidth(200f), 0.01f)
    }

    @Test
    fun centralAreaWorksInPortraitAndLandscapeWithoutCapturingEdges() {
        assertTrue(isPlaybackCenter(200f, 400f, 400, 800))
        assertTrue(isPlaybackCenter(400f, 200f, 800, 400))
        assertFalse(isPlaybackCenter(10f, 400f, 400, 800))
        assertFalse(isPlaybackCenter(200f, 790f, 400, 800))
        assertFalse(isPlaybackCenter(0f, 0f, 0, 0))
    }
}
