package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturesTest {
    @Test
    fun centerAlternatesPlaybackAndCanRestartOrRetry() {
        assertEquals(CenterPlaybackAction.PAUSE, centerPlaybackAction(hasError = false, isEnded = false, wantsToPlay = true))
        assertEquals(CenterPlaybackAction.PLAY, centerPlaybackAction(hasError = false, isEnded = false, wantsToPlay = false))
        assertEquals(CenterPlaybackAction.RESTART, centerPlaybackAction(hasError = false, isEnded = true, wantsToPlay = false))
        assertEquals(CenterPlaybackAction.RETRY, centerPlaybackAction(hasError = true, isEnded = true, wantsToPlay = false))
    }

    @Test
    fun controlsFitCompactPhonesAndKeepLargeTargets() {
        for (width in listOf(240f, 320f, 360f)) {
            val sizes = playerControlSizes(width, isTv = false)
            assertTrue(sizes.side >= 44f)
            assertTrue(sizes.center >= 44f)
            assertTrue(sizes.side * 2 + sizes.center + sizes.spacing * 2 <= width - 24f)
        }
        assertEquals(72f, playerControlSizes(960f, isTv = true).center, 0.01f)
    }

    @Test
    fun centralAreaWorksInPortraitAndLandscapeWithoutCapturingEdges() {
        assertTrue(isPlaybackCenter(200f, 400f, 400, 800))
        assertTrue(isPlaybackCenter(400f, 200f, 800, 400))
        assertFalse(isPlaybackCenter(10f, 400f, 400, 800))
        assertFalse(isPlaybackCenter(200f, 790f, 400, 800))
        assertFalse(isPlaybackCenter(0f, 0f, 0, 0))
    }

    @Test
    fun playbackTimesHandleUnknownDurationsAndHours() {
        assertEquals("00:00", formatPlaybackTime(-1))
        assertEquals("01:05", formatPlaybackTime(65_000))
        assertEquals("1:01:01", formatPlaybackTime(3_661_000))
    }

    @Test
    fun vlcReceivesSafeUserAgentAndReferrerBeforeMediaAssignment() {
        assertEquals(
            listOf(":http-user-agent=Agent/2.0", ":http-referrer=https://example.org/watch"),
            vlcMediaOptions(" Agent/2.0 ", " https://example.org/watch ")
        )
        assertEquals(listOf(":http-user-agent=SamPlay/1.0"), vlcMediaOptions(null, null))
        assertEquals(
            listOf(":http-user-agent=SamPlay/1.0"),
            vlcMediaOptions("Bad\r\n:network-caching=0", "https://example.org/\nInjected")
        )
        assertEquals(listOf(":http-user-agent=SamPlay/1.0"), vlcMediaOptions("x".repeat(513), "\u0000bad"))
    }
}
