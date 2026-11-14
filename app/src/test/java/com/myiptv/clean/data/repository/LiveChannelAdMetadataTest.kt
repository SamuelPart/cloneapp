package com.myiptv.clean.data.repository

import com.myiptv.clean.ads.canOfferLiveChannelAds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelAdMetadataTest {
    @Test fun secondaryCategoriesAreRetainedAndCanProtectKidsChannels() {
        val categories = sourceChannelCategories(listOf(" news ", "kids", "news", " "))
        assertEquals(listOf("news", "kids"), categories)
        assertFalse(canOfferLiveChannelAds(categories))
        assertTrue(sourceChannelCategories(null).isEmpty())
        assertFalse(canOfferLiveChannelAds(listOf("N/A", "Unclassified")))
    }

    @Test fun tdtPreservesProviderCategoryAndFailsClosedForKidsOrMissingMetadata() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="Noticias",Noticias reales
            https://example.com/news.m3u8
            #EXTINF:-1 group-title="Infantiles / Familia",Canal infantil
            https://example.com/kids.m3u8
            #EXTINF:-1,Canal sin categoría
            https://example.com/unknown.m3u8
        """.trimIndent()
        val (news, kids, unknown) = TdtChannelsRepository.parsePlaylist(playlist)
        assertEquals("Noticias", news.category)
        assertEquals(listOf("Noticias"), news.sourceCategories)
        assertTrue(canOfferLiveChannelAds(news.sourceCategories))
        assertEquals(listOf("Infantiles / Familia"), kids.sourceCategories)
        assertFalse(canOfferLiveChannelAds(kids.sourceCategories))
        assertTrue(unknown.sourceCategories.isEmpty())
        assertFalse(canOfferLiveChannelAds(unknown.sourceCategories))
    }
}
