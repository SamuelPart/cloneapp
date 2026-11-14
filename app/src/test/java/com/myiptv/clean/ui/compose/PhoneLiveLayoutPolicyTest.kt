package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneLiveLayoutPolicyTest {
    @Test fun rotatingOrWideningAMobileChannelScreenNeverReplacesItsPlayerWithTvUi() {
        assertFalse(useTvContentLayout(false, false, true, 360f))
        assertFalse(useTvContentLayout(false, false, true, 900f))
        assertFalse(useTvContentLayout(false, false, true, 1_200f))
    }

    @Test fun portraitPreviewUsesAvailableWidthWithoutMakingShortScreensTooSmall() {
        assertEquals(202.5f, phoneLivePreviewHeightDp(360f, 500f, portrait = true, searchOpen = false), 0.01f)
        assertEquals(180f, phoneLivePreviewHeightDp(320f, 344f, portrait = true, searchOpen = false), 0.01f)
        // A constrained window reserves space for controls and still improves on 42% of height.
        assertEquals(180f, phoneLivePreviewHeightDp(360f, 300f, portrait = true, searchOpen = false), 0.01f)
        assertEquals(118f, phoneLivePreviewHeightDp(360f, 300f, portrait = true, searchOpen = true), 0.01f)
    }

    @Test fun previewNeverExceedsTheWindowAndLandscapeKeepsSpaceForTheList() {
        assertEquals(0f, phoneLivePreviewHeightDp(0f, 500f, portrait = true, searchOpen = false), 0.01f)
        assertEquals(0f, phoneLivePreviewHeightDp(320f, 0f, portrait = true, searchOpen = false), 0.01f)
        assertEquals(126f, phoneLivePreviewHeightDp(640f, 300f, portrait = false, searchOpen = false), 0.01f)
        assertEquals(80f, phoneLivePreviewHeightDp(640f, 200f, portrait = false, searchOpen = false), 0.01f)
        assertTrue(phoneLivePreviewHeightDp(320f, 70f, portrait = false, searchOpen = false) <= 70f)
    }

    @Test fun veryShortWindowsLeaveSpaceForTheControlsAndSearch() {
        assertEquals(80f, phoneLivePreviewHeightDp(360f, 200f, portrait = true, searchOpen = false), 0.01f)
        assertEquals(18f, phoneLivePreviewHeightDp(360f, 200f, portrait = true, searchOpen = true), 0.01f)
        assertEquals(18f, phoneLivePreviewHeightDp(640f, 200f, portrait = false, searchOpen = true), 0.01f)
        assertEquals(0f, phoneLivePreviewHeightDp(360f, 150f, portrait = true, searchOpen = true), 0.01f)
    }

    @Test fun previewStaysWithinVideoAndAvailableControlBounds() {
        for (width in listOf(200f, 320f, 360f, 640f, 1_000f)) {
            for (height in listOf(70f, 150f, 200f, 300f, 500f, 900f)) {
                for (portrait in listOf(true, false)) {
                    for (searchOpen in listOf(true, false)) {
                        val preview = phoneLivePreviewHeightDp(width, height, portrait, searchOpen)
                        val available = (height - if (searchOpen) 182f else 120f).coerceAtLeast(0f)
                        assertTrue(preview >= 0f)
                        assertTrue(preview <= width * 9f / 16f)
                        assertTrue(preview <= available)
                    }
                }
            }
        }
    }

    @Test fun boundsAdjustmentOnlyAffectsThePhoneLivePlayer() {
        assertTrue(usePhoneLiveVideoBounds(isTv = false, hasPhoneLiveActions = true))
        assertFalse(usePhoneLiveVideoBounds(isTv = false, hasPhoneLiveActions = false))
        assertFalse(usePhoneLiveVideoBounds(isTv = true, hasPhoneLiveActions = true))
        assertFalse(usePhoneLiveVideoBounds(isTv = true, hasPhoneLiveActions = false))
    }

    @Test fun televisionAndOtherExistingLayoutsKeepTheirBehavior() {
        assertTrue(useTvContentLayout(true, false, true, 360f))
        assertTrue(useTvContentLayout(true, true, false, 360f))
        assertTrue(useTvContentLayout(false, false, false, 840f))
        assertFalse(useTvContentLayout(false, false, false, 839f))
        assertFalse(useTvContentLayout(false, true, false, 1_200f))
    }
}
