package com.myiptv.clean.data.repository

import com.myiptv.clean.data.model.IptvOrgLogo
import com.myiptv.clean.ui.compose.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelLogoCatalogTest {
    private fun logo(channel: String = "Canal.co", feed: String? = null, format: String = "PNG", inUse: Boolean = true, url: String = "https://images.example/logo.png") =
        IptvOrgLogo(channel, feed, inUse, format, url)

    private fun channel(feed: String? = null, suppliedLogo: String? = null, id: String? = "Canal.co") =
        LiveChannel("iptv_1", "Canal", "General", "C", "https://example.com/live.m3u8", 0L, logoUrl = suppliedLogo, epgId = id, feedId = feed)

    @Test fun prefersTheCatalogLogoThenExactFeedThenGeneralLogo() {
        val logos = selectChannelLogos(listOf(
            logo(url = "https://images.example/general.png"),
            logo(feed = "HD", url = "https://images.example/hd.png"),
            logo(feed = "Other", url = "https://images.example/other.png")
        ))
        assertEquals("https://images.example/hd.png", channelLogoUrl(channel("HD"), logos))
        assertEquals("https://images.example/general.png", channelLogoUrl(channel("SD"), logos))
        assertEquals("https://images.example/provided.png", channelLogoUrl(channel("HD", "https://images.example/provided.png"), logos))
        assertEquals("https://images.example/hd.png", channelLogoUrl(channel("HD", "file:///private/image.png"), logos))
        assertNull(channelLogoUrl(channel(id = null), logos))
    }

    @Test fun rejectsInactiveUnsupportedAndUnsafeImagesAndNeverBorrowsAnotherFeed() {
        val logos = selectChannelLogos(listOf(
            logo(inUse = false),
            logo(format = "SVG", url = "https://images.example/logo.svg"),
            logo(url = "file:///private/image.png"),
            logo(url = "https://images.example/bad\nheader"),
            logo(feed = "Other", url = "https://images.example/other.png")
        ))
        assertEquals(1, logos.size)
        assertNull(channelLogoUrl(channel("HD"), logos))
        assertNull(channelLogoUrl(channel(), logos))
    }

    @Test fun firstValidCurrentLogoWins() {
        val logos = selectChannelLogos(listOf(
            logo(format = "svg"),
            logo(format = "jpeg", url = "https://images.example/first.jpg"),
            logo(format = "PNG", url = "https://images.example/second.png")
        ))
        assertEquals("https://images.example/first.jpg", channelLogoUrl(channel(), logos))
    }
}
