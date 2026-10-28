package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbedNavigationPolicyTest {
    private val policy = EmbedNavigationPolicy("https://nupload.my/watch/video")

    @Test
    fun onlyProviderCanNavigateTheTopLevelPage() {
        assertTrue(policy.allowsNavigation("https://nupload.my/watch/next", true))
        assertTrue(policy.allowsNavigation("https://www.nupload.my/embed/next", true))
        assertFalse(policy.allowsNavigation("https://nupload.my.evil.example/watch", true))
        assertFalse(policy.allowsNavigation("https://different.example/player", true))
        assertFalse(policy.allowsNavigation("http://nupload.my/watch/next", true))
        assertFalse(policy.allowsNavigation("intent://other.example", true))
        assertFalse(policy.allowsNavigation("https://user@nupload.my/watch/next", true))
    }

    @Test
    fun embeddedVideoFramesCanUseOtherHostsExceptKnownAdNetworks() {
        assertTrue(policy.allowsNavigation("https://video.example/embed/123", false))
        assertTrue(policy.allowsNavigation("about:blank", false))
        assertTrue(policy.allowsNavigation("blob:https://nupload.my/video", false))
        assertFalse(policy.allowsNavigation("https://ads.doubleclick.net/frame", false))
        assertFalse(policy.allowsNavigation("javascript:alert(1)", false))
    }

    @Test
    fun adBlockListMatchesDomainBoundariesNotSubstrings() {
        assertTrue(policy.isAdRequest("https://doubleclick.net/ads"))
        assertTrue(policy.isAdRequest("https://a.doubleclick.net/ads"))
        assertTrue(policy.isAdRequest("https://GOOGLESYNDICATION.COM/ads"))
        assertFalse(policy.isAdRequest("https://doubleclick.net.example.org/video"))
        assertFalse(policy.isAdRequest("https://notdoubleclick.net/video"))
        assertFalse(policy.isAdRequest("not a url"))
    }

    @Test
    fun explicitApprovalIsExactAndCannotWhitelistKnownAdsOrDowngradeHttps() {
        assertEquals("video.example", policy.approvableHost("https://video.example/embed/2"))
        val approved = setOf("video.example")
        assertTrue(policy.allowsNavigation("https://video.example/embed/2", true, approved))
        assertFalse(policy.allowsNavigation("https://other.video.example/embed/2", true, approved))
        assertNull(policy.approvableHost("https://ads.doubleclick.net/page"))
        assertNull(policy.approvableHost("http://video.example/embed/2"))
        assertFalse(policy.allowsNavigation("https://ads.doubleclick.net/page", true, setOf("ads.doubleclick.net")))
        assertFalse(policy.allowsNavigation("http://video.example/embed/2", true, approved))
    }

    @Test
    fun wwwSourceAllowsTheSameProviderWithoutItsPrefix() {
        val wwwPolicy = EmbedNavigationPolicy("https://www.example.org/watch/1")
        assertTrue(wwwPolicy.allowsNavigation("https://example.org/watch/2", true))
        assertTrue(wwwPolicy.allowsNavigation("https://player.www.example.org/embed/2", true))
        assertFalse(wwwPolicy.allowsNavigation("https://player.example.org/embed/2", true))
        assertFalse(wwwPolicy.allowsNavigation("https://example.org.attacker.test/", true))
        val suffixPolicy = EmbedNavigationPolicy("https://www.co.uk/watch/1")
        assertFalse(suffixPolicy.allowsNavigation("https://unrelated.co.uk/watch/2", true))
    }
}
