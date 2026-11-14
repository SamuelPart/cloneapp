package com.myiptv.clean.ui.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeToEdgePolicyTest {
    @Test fun forcedEdgeToEdgeProtectsPlayBrowsingOnNewAndroid() {
        assertTrue(needsSafeArea(deviceApi = 35, targetApi = 36, isTelevision = false, immersive = false))
        assertTrue(needsSafeArea(deviceApi = 36, targetApi = 36, isTelevision = false, immersive = false))
    }

    @Test fun oldAndroidAndGithubTargetKeepTheirExistingLayout() {
        assertFalse(needsSafeArea(deviceApi = 34, targetApi = 36, isTelevision = false, immersive = false))
        assertFalse(needsSafeArea(deviceApi = 36, targetApi = 34, isTelevision = false, immersive = false))
    }

    @Test fun tvAndFullscreenPlaybackRemainImmersive() {
        assertFalse(needsSafeArea(deviceApi = 36, targetApi = 36, isTelevision = true, immersive = false))
        assertFalse(needsSafeArea(deviceApi = 36, targetApi = 36, isTelevision = false, immersive = true))
    }
}
