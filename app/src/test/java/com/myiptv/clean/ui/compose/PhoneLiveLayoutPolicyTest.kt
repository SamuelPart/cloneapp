package com.myiptv.clean.ui.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneLiveLayoutPolicyTest {
    @Test fun rotatingOrWideningAMobileChannelScreenNeverReplacesItsPlayerWithTvUi() {
        assertFalse(useTvContentLayout(false, false, true, 360f))
        assertFalse(useTvContentLayout(false, false, true, 900f))
        assertFalse(useTvContentLayout(false, false, true, 1_200f))
    }

    @Test fun televisionAndOtherExistingLayoutsKeepTheirBehavior() {
        assertTrue(useTvContentLayout(true, false, true, 360f))
        assertTrue(useTvContentLayout(true, true, false, 360f))
        assertTrue(useTvContentLayout(false, false, false, 840f))
        assertFalse(useTvContentLayout(false, false, false, 839f))
        assertFalse(useTvContentLayout(false, true, false, 1_200f))
    }
}
